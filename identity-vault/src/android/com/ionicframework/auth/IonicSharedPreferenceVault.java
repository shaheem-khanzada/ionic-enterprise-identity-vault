package com.ionicframework.auth;

import android.content.Context;
import android.util.Log;

import com.bottlerocketstudios.vault.StandardSharedPreferenceVault;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import javax.crypto.SecretKey;


public class IonicSharedPreferenceVault extends StandardSharedPreferenceVault implements IonicVault {
    final private String TAG = "IonicSPVault";
    final private IonicKeyStorage mKeyStorage;
    final private String DATA_KEY = "DATA";
    final private String VALIDATION_KEY = "ValidKey";
    private Context mContext;

    IonicSharedPreferenceVault(Context context, IonicKeyStorage keyStorage, String prefFileName, String transform, boolean enableExceptions) {
        super(context, keyStorage, prefFileName, transform, enableExceptions);
        mKeyStorage = keyStorage;
        mContext = context;
    }

    @Override
    public void storeValue(String key, Object obj) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        JSONObject data;
        try {
            data = getDataObj();
        } catch (VaultError e) {
            Log.d(TAG, "Clearing malformed data obj in vault.");
            data = new JSONObject();
        }
        try {
            data.put(key, obj);
            if (data.isNull(key)) {
                data.remove(key);
            }
        } catch (JSONException e) {
            throw new VaultError("Error storing value");
        }
        storeDataObj(data);
    }

    @Override
    public void removeValue(String key) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        JSONObject data;
        try {
            data = getDataObj();
        } catch (VaultError e) {
            Log.d(TAG, "Clearing malformed data obj in vault.");
            data = new JSONObject();
        }

        data.remove(key);

        storeDataObj(data);
    }

    private void storeDataObj(JSONObject data) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        this.edit().putString(DATA_KEY, data.toString()).apply();
    }

    @Override
    public Object getStoredValue(String key) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        JSONObject data = getDataObj();
        return data.opt(key);
    }

    @Override
    public JSONArray getKeys() throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        JSONObject data = getDataObj();
        JSONArray returnKeys = new JSONArray();
        Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            returnKeys.put(keys.next());
        }
        return returnKeys;
    }

    @Override
    public JSONObject getDataObj() throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        try {
            String dataString = this.getString(DATA_KEY, null);
            if (dataString == null) {
                return new JSONObject();
            }
            return new JSONObject(dataString);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private void setValidationKey() {
       this.edit().putString(VALIDATION_KEY, VALIDATION_KEY).apply();
    }

    @Override
    public void validateLogin() throws AuthFailedError {
        try {
            String storedValidation = this.getString(VALIDATION_KEY, null);
            if (storedValidation == null || !storedValidation.equals(VALIDATION_KEY)) {
                mKeyStorage.lock();
                throw new AuthFailedError();
            }
        } catch (Exception e) {
            throw new AuthFailedError();
        }
    }

    @Override
    public void rekeyStorage(SecretKey secretKey) {
        clearStorage();
        setKey(secretKey);
        setValidationKey();
    }

    @Override
    public boolean isLocked() {
       return mKeyStorage.isLocked();
    }

    @Override
    public void lock() {
        mKeyStorage.lock();
    }

    @Override
    public SecretKey getKey() {
        return mKeyStorage.loadKey(mContext);
    }

    @Override
    public void restoreVaultWithNewKey(SecretKey key) throws VaultError {
        if (isLocked()) { throw new VaultLockedError(); }
        JSONObject previousData = getDataObj();
        rekeyStorage(key);
        storeDataObj(previousData);
    }
}
