package com.ionicframework.auth;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import com.bottlerocketstudios.vault.EncryptionConstants;
import com.bottlerocketstudios.vault.SharedPreferenceVault;
import com.bottlerocketstudios.vault.SharedPreferenceVaultFactory;

import java.security.GeneralSecurityException;

public class VaultFactory extends SharedPreferenceVaultFactory {

    final private static String CONFIG_PREF_FILE = "_ivConfigPrefFile";
    final private static String CONFIG_KEY_FILE = "_ivConfigKeyFile";
    final private static String CONFIG_KEY_ALIAS = "_ivConfigKeyAlias";
    final private static String CONFIG_APP_SECRET = "_ivConfigAppSecret";
    final private static Integer CONFIG_KEY_INDEX = 0;
    final private static String PASSCODE_PREF_NAME = "%s:passcode:pref";
    final private static String BIOMETRIC_PREF_NAME = "%s:biometric:pref";
    final private static String BIOMETRIC_KEY_NAME = "%s:biometric:key";

    /**
     * Create a vault that uses the operating system's built in keystore locking mechanism. Whenever
     * the device has not been unlocked in a specified amount of time, reading from this vault will
     *
     * NOTE: This swaps out the standard KeychainStorage for a version that keeps the key in memory once unlocked so the user doesn't need to
     * reauth every time they read or write until the vault is locked
     *
     * throw a {@link android.security.keystore.KeyPermanentlyInvalidatedException} or {@link android.security.keystore.UserNotAuthenticatedException}.
     *
     * @param context                Application context.
     * @param prefFileName           Preference file name to be used for storage of data.
     * @param keyAlias               Alias of Keystore key, must be unique within application.
     * @param authDurationSeconds    Time in seconds to allow use of the key without requiring authentication.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private static IonicVault getInMemKeychainVault(Context context, String prefFileName, String keyAlias, int authDurationSeconds) {
        IonicKeyStorage keyStorage = new IonicKeychainAuthenticatedStorage(keyAlias, EncryptionConstants.AES_CIPHER, EncryptionConstants.BLOCK_MODE_CBC, EncryptionConstants.ENCRYPTION_PADDING_PKCS7, authDurationSeconds);

        IonicVault ionicVault = new IonicSharedPreferenceVault(context, keyStorage, prefFileName, EncryptionConstants.AES_CBC_PADDED_TRANSFORM_ANDROID_M, true);
        if (!ionicVault.isKeyAvailable()) {
            ionicVault.rekeyStorage(null);
        }
        return ionicVault;
    }

    public static IonicVault getPasscodeVault(Context context, String descriptor) {
        IonicKeyStorage keyStorage = new IonicMemoryOnlyKeyStorage();
        return new IonicSharedPreferenceVault(context, keyStorage, String.format(PASSCODE_PREF_NAME, descriptor), EncryptionConstants.AES_CBC_PADDED_TRANSFORM, true);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static IonicVault getBiometricVault(Context context, String descriptor) {
        return VaultFactory.getInMemKeychainVault(
                context,
                String.format(BIOMETRIC_PREF_NAME, descriptor),
                String.format(BIOMETRIC_KEY_NAME, descriptor),
                30
        );
    }

    public static IonicStateVault getStateVault(Context context) throws VaultError {
        try {
            SharedPreferenceVault vault = VaultFactory.getAppKeyedCompatAes256Vault(
                    context,
                    CONFIG_PREF_FILE,
                    CONFIG_KEY_FILE,
                    CONFIG_KEY_ALIAS,
                    CONFIG_KEY_INDEX,
                    CONFIG_APP_SECRET,
                    true
            );
            return new IonicStateVault(vault);
        } catch (GeneralSecurityException e) {
            throw new VaultError("Error creating config vault.");
        }
    }
}
