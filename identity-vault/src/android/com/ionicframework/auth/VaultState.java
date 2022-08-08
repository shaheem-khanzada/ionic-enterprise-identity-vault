package com.ionicframework.auth;

import android.util.Base64;

import com.bottlerocketstudios.vault.keys.generator.Aes256KeyFromPasswordFactory;
import com.bottlerocketstudios.vault.keys.generator.Aes256RandomKeyFactory;
import com.bottlerocketstudios.vault.salt.PrngSaltGenerator;
import com.bottlerocketstudios.vault.salt.SpecificSaltGenerator;

import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class VaultState {

    final private String BIO_FLAG = "biometricsEnabled";
    final private String PASSCODE_FLAG = "passcodeEnabled";
    final private String PASSCODE_SETUP_FLAG = "passcodeSetup";
    final private String SECURE_STORAGE_MODE_FLAG = "secureStorageModeEnabled";
    final private String SECURE_STORAGE_KEY = "secureStorageKey";
    final private String SECURE_STORAGE_KEY_ALGORITHM = "STORAGE_KEY_ALGORITHM";
    final private String IN_USE_FLAG = "inUse";
    final private String SALT_KEY = "salt";
    final private IonicStateVault mStateVault;
    final private String mDescriptor;
    private boolean secureStorageModeEnabled;
    public boolean biometricsEnabled;
    public boolean passcodeEnabled;
    public SecretKey secureStorageKey;
    public boolean passcodeSetup;
    public boolean inUse;
    public byte[] salt;

    VaultState(IonicStateVault stateVault, String descriptor, boolean isBioAvailable) {
        mStateVault = stateVault;
        mDescriptor = descriptor;
        JSONObject previousState = mStateVault.getState(mDescriptor);
        if (previousState != null) {
            biometricsEnabled = previousState.optBoolean(BIO_FLAG, isBioAvailable);
            passcodeEnabled = previousState.optBoolean(PASSCODE_FLAG, false);
            secureStorageModeEnabled = previousState.optBoolean(SECURE_STORAGE_MODE_FLAG, false);
            if (secureStorageModeEnabled) {
                String encodedKey = previousState.optString(SECURE_STORAGE_KEY, null);
                String keyAlgorithm = previousState.optString(SECURE_STORAGE_KEY_ALGORITHM, null);
                if (encodedKey != null) {
                    byte[] decodedKey = Base64.decode(encodedKey, Base64.DEFAULT);
                    secureStorageKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, keyAlgorithm);
                }
            }
            inUse = previousState.optBoolean(IN_USE_FLAG, true);
            passcodeSetup = previousState.optBoolean(PASSCODE_SETUP_FLAG, false);
            String encodedSalt = previousState.optString(SALT_KEY, null);
            if (encodedSalt != null) {
                salt = Base64.decode(encodedSalt, Base64.DEFAULT);
            }

            return;
        }
        biometricsEnabled = isBioAvailable;
        passcodeEnabled = false;
        passcodeSetup = false;
        secureStorageModeEnabled = false;
        inUse = false;
    }

    public void newSalt() {
        PrngSaltGenerator prngSaltGenerator = new PrngSaltGenerator();
        salt = prngSaltGenerator.createSaltBytes(Aes256KeyFromPasswordFactory.SALT_SIZE_BYTES);
    }

    public void enableSecureStorage(boolean enable) throws VaultError {
        if (secureStorageModeEnabled == enable) { return; }
        secureStorageModeEnabled = enable;
        if (secureStorageModeEnabled) {
            secureStorageKey = Aes256RandomKeyFactory.createKey();
        } else {
            secureStorageKey = null;
        }
        storeState();
    }

    public boolean isSecureStorageModeEnabled() {
        return secureStorageModeEnabled;
    }

    public SpecificSaltGenerator getSaltGenerator() {
        if (salt == null) {
            newSalt();
        }
        return new SpecificSaltGenerator(salt);
    }

    public void storeState() throws  VaultError {
        try {
            JSONObject state = new JSONObject();
            state.put(BIO_FLAG, biometricsEnabled);
            state.put(PASSCODE_FLAG, passcodeEnabled);
            state.put(IN_USE_FLAG, inUse);
            state.put(PASSCODE_SETUP_FLAG, passcodeSetup);
            state.put(SECURE_STORAGE_MODE_FLAG, secureStorageModeEnabled);


            if (secureStorageModeEnabled) {
                String encodedKey = Base64.encodeToString(secureStorageKey.getEncoded(), Base64.DEFAULT);
                state.put(SECURE_STORAGE_KEY, encodedKey);
                state.put(SECURE_STORAGE_KEY_ALGORITHM, secureStorageKey.getAlgorithm());
            } else {
                state.remove(SECURE_STORAGE_KEY);
                state.remove(SECURE_STORAGE_KEY_ALGORITHM);
            }

            if (salt != null) {
                String encodedSalt = Base64.encodeToString(salt, Base64.DEFAULT);
                state.put(SALT_KEY, encodedSalt);
            }
            mStateVault.storeState(mDescriptor, state);
        } catch (JSONException e) {
            throw new VaultError("Error storing state configuration");
        }
    }


}
