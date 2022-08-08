package com.ionicframework.auth;

import com.bottlerocketstudios.vault.SharedPreferenceVault;

import org.json.JSONException;
import org.json.JSONObject;

public class IonicStateVault {

    private SharedPreferenceVault mVault;

    IonicStateVault(SharedPreferenceVault vault) {
        mVault = vault;
    }

    public void storeState(String key, JSONObject obj) {
        mVault.edit().putString(key, obj.toString()).apply();
    }

    public JSONObject getState(String key) {
        String val = mVault.getString(key, null);
        if (val == null) {
            return null;
        }
        try {
            return new JSONObject(val);
        } catch (JSONException e) {
            return null;
        }
    }
}
