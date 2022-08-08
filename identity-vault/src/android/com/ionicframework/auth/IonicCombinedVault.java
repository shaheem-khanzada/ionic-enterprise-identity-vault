package com.ionicframework.auth;

import android.annotation.TargetApi;
import android.content.Context;

import com.bottlerocketstudios.vault.EncryptionConstants;
import com.bottlerocketstudios.vault.keys.generator.Aes256KeyFromPasswordFactory;
import com.bottlerocketstudios.vault.keys.generator.Aes256RandomKeyFactory;
import com.bottlerocketstudios.vault.salt.SaltGenerator;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/*
    Vault that combines passcode & biometric vaults depending on the configuration
 */
public class IonicCombinedVault {
    private static final int PBKDF_ITERATIONS = 10000;
    final private int MAX_AUTH_ATTEMPTS = 5;
    final private String STORAGE_KEY = "STORAGE_KEY";
    final private String STORAGE_KEY_ALGORITHM = "STORAGE_KEY_ALGORITHM";
    final private String TAG = "IonicCombinedVault";
    final private String FINGERPRINT_KEY = "_ionicAuthFingerprintKey";


    private IonicVault mStorageVault;
    private IonicVault mBiometricVault;
    private IdentityVault mParent;
    private VaultState mState;
    private Context mContext;
    private String mDescriptor;
    private int remainingAttempts = MAX_AUTH_ATTEMPTS;

    IonicCombinedVault(Context context, String descriptor, IdentityVault parent) throws VaultError {
       mDescriptor = descriptor;
       mParent = parent;
       mState = new VaultState(VaultFactory.getStateVault(context), descriptor, parent.isBiometricsAvailable());
       mContext = context;
       mStorageVault = VaultFactory.getPasscodeVault(context, mDescriptor);
       if (parent.isBiometricsAvailable()) {
           mBiometricVault = VaultFactory.getBiometricVault(context, mDescriptor);
       }
       autoGenerateKeyIfNeeded();
    }

    private void markAsInUse() throws VaultError {
        if (mState.inUse) { return; }
        mState.inUse = true;
        mState.storeState();
    }

    public boolean isLocked() {
        return mStorageVault.isLocked() && mState.inUse && !mState.isSecureStorageModeEnabled();
    }

    public void lock() {
        if (isLocked()) { return; }
        if (!isInUse()) { return; }
        if (isSecureStorageModeEnabled()) { return; }
        if (!mState.passcodeEnabled && !mState.biometricsEnabled) {
            // NOTE: this means we're using it as an in memory only store
            try {
                clear();
            } catch (VaultError e) { }
        }
        mStorageVault.lock();
        if (mBiometricVault != null) {
            mBiometricVault.lock();
        }
    }

    public boolean isBiometricsEnabled() {
        return mState.biometricsEnabled && mParent.isBiometricsAvailable();
    }

    public boolean isPasscodeEnabled() {
        return mState.passcodeEnabled;
    }

    public boolean isSecureStorageModeEnabled() {
        return mState.isSecureStorageModeEnabled();
    }

    public boolean isInUse() {
        return mState.inUse;
    }

    public Object getStoredValue(String key) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        if (!isInUse()) { return null; }
        return mStorageVault.getStoredValue(key);
    }

    public JSONArray getKeys() throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        if (!isInUse()) { return null; }
        return mStorageVault.getKeys();
    }

    public void storeValue(String key, Object obj) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        if (needsUserPasswordSetup()) { throw new MissingPasscodeError(); }
        mStorageVault.storeValue(key, obj);
        markAsInUse();
    }

    public void removeValue(String key) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        if (needsUserPasswordSetup()) { throw new MissingPasscodeError(); }
        mStorageVault.removeValue(key);
        markAsInUse();
    }

    public void setPasscodeEnabled(boolean enabled) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        if (enabled == isPasscodeEnabled()) { return; }
        mState.passcodeEnabled = enabled;
        if (enabled) {
            setSecureStorageModeEnabled(false);
            mState.passcodeSetup = false;
            mState.storeState();
            autoGenerateKeyIfNeeded();
        } else {
            mState.salt = null;
            SecretKey key = Aes256RandomKeyFactory.createKey();
            mStorageVault.restoreVaultWithNewKey(key);
            mState.passcodeSetup = true;
            mState.storeState();
            storeKeyInBioVault(key);
        }
    }

    public void setBiometricsEnabled(boolean enabled) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        if (enabled == isBiometricsEnabled()) { return; }
        mState.biometricsEnabled = enabled;
        mState.storeState();
        if (enabled) {
            setupBiometricChangeDetectionKey();
            setSecureStorageModeEnabled(false);
            storeKeyInBioVault(mStorageVault.getKey());
        } else {
            if (mBiometricVault != null) {
                mBiometricVault.rekeyStorage(null);
            }
        }
    }

    public void setSecureStorageModeEnabled(boolean enabled) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        if (enabled == isSecureStorageModeEnabled()) { return; }
        if (enabled) {
            setBiometricsEnabled(false);
            setPasscodeEnabled(false);
            mState.enableSecureStorage(true);
            mStorageVault.restoreVaultWithNewKey(mState.secureStorageKey);
        } else {
            mState.enableSecureStorage(false);
            SecretKey key = Aes256RandomKeyFactory.createKey();
            mStorageVault.restoreVaultWithNewKey(key);
        }
        mState.storeState();
    }

    public void clear() throws VaultError {
        if (mBiometricVault != null) {
            mBiometricVault.rekeyStorage(null);
        }
        mStorageVault.clearStorage();
        mState.inUse = false;
        mState.passcodeSetup = false;
        mState.storeState();
        autoGenerateKeyIfNeeded();
    }

    private void autoGenerateKeyIfNeeded() {
        if (mStorageVault.isKeyAvailable()) { return; } // don't if the key is ready
        if (mState.inUse && !mState.isSecureStorageModeEnabled()) { return; } // don't generate if inUse (force user to clear)
        if (mState.isSecureStorageModeEnabled()) {
            mStorageVault.setKey(mState.secureStorageKey);
        } else {
            SecretKey key = Aes256RandomKeyFactory.createKey();
            mStorageVault.rekeyStorage(key);
        }
        storeKeyInBioVault(mStorageVault.getKey());
    }

    private void storeKeyInBioVault(SecretKey key) {
        if (isBiometricsEnabled()) {
            if (mBiometricVault == null) {
                mBiometricVault = VaultFactory.getBiometricVault(mContext, mDescriptor);
            }
            String encodedKey = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
            mBiometricVault.rekeyStorage(null);
            mBiometricVault
                    .edit()
                    .putString(STORAGE_KEY, encodedKey)
                    .putString(STORAGE_KEY_ALGORITHM, key.getAlgorithm())
                    .apply();
        }
    }

    public boolean needsUserPasswordSetup() {
        return !mState.passcodeSetup && mState.passcodeEnabled;
    }

    private SecretKey keyFromPassword(String password, SaltGenerator saltGenerator) {
        return Aes256KeyFromPasswordFactory.createKey(password, PBKDF_ITERATIONS, saltGenerator);
    }

    public void setPasscode(String password) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        if (!mState.passcodeEnabled) { throw new PasscodeNotEnabledError(); }
        mState.newSalt();
        SecretKey key = keyFromPassword(password, mState.getSaltGenerator());
        mStorageVault.restoreVaultWithNewKey(key);
        mState.passcodeSetup = true;
        mState.storeState();
        storeKeyInBioVault(key);
    }

    public void unlock(String password) throws VaultError {
        if (!isLocked()) { return; }
        if (!mState.passcodeEnabled) { throw new PasscodeNotEnabledError(); }
        SecretKey key = Aes256KeyFromPasswordFactory.createKey(password, PBKDF_ITERATIONS, mState.getSaltGenerator());
        mStorageVault.setKey(key);
        try {
            mStorageVault.validateLogin();
            remainingAttempts = MAX_AUTH_ATTEMPTS;
            storeKeyInBioVault(mStorageVault.getKey());
        } catch (AuthFailedError e) {
            remainingAttempts--;
            mStorageVault.lock();
            if (remainingAttempts == 0) {
                throw new TooManyFailedAttemptsError();
            }
            throw e;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean handleRuntimeException(Throwable throwable) throws VaultError {
        if (throwable instanceof UserNotAuthenticatedException) {
            Log.i(TAG, "User authentication expired");
            throw new AuthFailedError();
        } else if (throwable instanceof KeyPermanentlyInvalidatedException) {
            Log.i(TAG, "User changed unlock code and permanently invalidated the key");
            clear();
            throw new InvalidatedCredentialsError();
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setupBiometricChangeDetectionKey() throws VaultError {
        try {
            KeyStore keyStore = KeyStore.getInstance(EncryptionConstants.ANDROID_KEY_STORE);
            keyStore.load(null);
            keyStore.deleteEntry(FINGERPRINT_KEY);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(EncryptionConstants.AES_CIPHER, EncryptionConstants.ANDROID_KEY_STORE);

            keyGenerator.init(new KeyGenParameterSpec.Builder(FINGERPRINT_KEY, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
            Log.d(TAG, "set up key");
        } catch (Exception e) {
            throw new VaultError(e.getLocalizedMessage());
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void detectFingerprintsChanged() throws VaultError {
        try {
            KeyStore keyStore = KeyStore.getInstance(EncryptionConstants.ANDROID_KEY_STORE);
            keyStore.load(null);
            Key key = keyStore.getKey(FINGERPRINT_KEY, null);
            if (key == null) {
                setupBiometricChangeDetectionKey();
                return;
            }
            Cipher cipher = Cipher.getInstance(EncryptionConstants.AES_CBC_PADDED_TRANSFORM_ANDROID_M);
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (KeyPermanentlyInvalidatedException e) {
            if (!isPasscodeEnabled()) {
                // no way for them to recover so might as well clear the vault
                clear();
            }
            mBiometricVault.rekeyStorage(null);
            setupBiometricChangeDetectionKey();
            throw new InvalidatedCredentialsError();
        } catch (Exception e) {
            throw new VaultError(e.getLocalizedMessage());
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void unlock() throws VaultError {
        if (!isLocked()) { return; }
        if (!isBiometricsEnabled()) { throw new BiometricsNotEnabled(); }
        detectFingerprintsChanged();
        try {
            String encodedKey = mBiometricVault.getString(STORAGE_KEY, null);
            String keyAlgorithm = mBiometricVault.getString(STORAGE_KEY_ALGORITHM, null);
            if (encodedKey == null || keyAlgorithm == null) {
                mState.biometricsEnabled = false;
                mState.storeState();
                mBiometricVault.rekeyStorage(null);
                throw new BiometricsNotEnabled();
            }
            byte[] decodedKey = Base64.decode(encodedKey, Base64.DEFAULT);
            SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, keyAlgorithm);
            mStorageVault.setKey(key);
        } catch (RuntimeException e) {
            if (!handleRuntimeException(e.getCause())) {
                Log.e(TAG, "Failed to handle exception", e);
                throw new VaultError("unhandled runtime error: " + e.getLocalizedMessage());
            }
        }

        try {
            mStorageVault.validateLogin();
            remainingAttempts = MAX_AUTH_ATTEMPTS;
        } catch (AuthFailedError e) {
            remainingAttempts--;
            mStorageVault.lock();
            if (remainingAttempts == 0) {
                throw new TooManyFailedAttemptsError();
            }
            throw e;
        }
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}
