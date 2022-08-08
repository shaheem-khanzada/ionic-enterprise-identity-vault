package com.ionicframework.auth;

import org.json.JSONException;
import org.json.JSONObject;

public class VaultDescriptor {
    final public String username;
    final public String vaultId;
    final private String uniqueId = "%s:%s";

    VaultDescriptor(JSONObject config) throws VaultError {
        try {
            vaultId = config.getString("vaultId");
            username = config.getString("username");
        } catch (JSONException e) {
            throw new InvalidArgumentsError("vaultId or username missing");
        }
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("username", username);
        j.put("vaultId", vaultId);
        return j;
    }

    public String getUniqueId() {
        return String.format(uniqueId, username, vaultId);
    }
}

