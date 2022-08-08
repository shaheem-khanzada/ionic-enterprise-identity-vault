package com.ionicframework.auth;

import android.app.Activity;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthConfig {

    final public VaultDescriptor descriptor;
    final public VaultAppConfig appConfig;

    AuthConfig(JSONObject config, Activity activity, VaultDescriptor descriptor) {
        VaultAppConfig.configure(config, activity);
        appConfig = VaultAppConfig.getInstance();
        this.descriptor = descriptor;
    }

    public JSONObject toJSONObject() throws VaultError {
        try {
            JSONObject j = new JSONObject();
            j.put("lockAfter", appConfig.lockAfter);
            j.put("hideScreenOnBackground", appConfig.hideScreenOnBackground);
            j.put("descriptor", descriptor.toJSONObject());
            return j;
        } catch (JSONException e) {
            throw new VaultError("Error converting config to JSON");
        }
    }
}

