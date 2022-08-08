package com.ionicframework.auth;

import com.bottlerocketstudios.vault.SharedPreferenceVault;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.SecretKey;

public interface IonicVault  extends SharedPreferenceVault {

    boolean isLocked();

    void storeValue(String key, Object obj) throws VaultError;

    void removeValue(String key) throws VaultError;

    Object getStoredValue(String key) throws VaultError;

    JSONArray getKeys() throws VaultError;

    void validateLogin() throws AuthFailedError;

    void lock();

    JSONObject getDataObj() throws VaultError;

    void restoreVaultWithNewKey(SecretKey key) throws VaultError;

    SecretKey getKey();
}
