package com.ionicframework.auth;

import android.app.Activity;
import android.content.Context;

import org.json.JSONObject;

/*
    A Singleton representing the App configuration which shouldn't change across
    individual vaults
 */
public class VaultAppConfig {

    private boolean configured = false;

    // How long to wait before forcing the user to log in again (0 disables this)
    public int lockAfter = 0;
    // Whether to automatically obscure the app when backgrounded
    public boolean hideScreenOnBackground = false;
    String promptTitle = "Please Authenticate";
    String promptSubtitle = null;
    String promptDescription = null;
    String promptNegativeButtonText = "Cancel";
    public boolean allowSystemPinFallback = false;
    public boolean shouldClearVaultAfterTooManyFailedAttempts = true;

    private Activity activity;

    private static class SingletonHolder {
        public static final VaultAppConfig instance = new VaultAppConfig();
    }


    public static VaultAppConfig getInstance() { return SingletonHolder.instance; }


    // configure the singleton once and only once
    public static void configure(JSONObject config, Activity activity) {
        VaultAppConfig instance = SingletonHolder.instance;
        if (instance.configured) {
            return;
        }
        instance.activity = activity;
        instance.promptTitle = config.optString("androidPromptTitle", instance.promptTitle);
        instance.promptSubtitle = config.optString("androidPromptSubtitle", instance.promptSubtitle);
        instance.promptDescription = config.optString("androidPromptDescription", instance.promptDescription);
        instance.promptNegativeButtonText = config.optString("androidPromptNegativeButtonText", instance.promptNegativeButtonText);
        instance.allowSystemPinFallback = config.optBoolean("allowSystemPinFallback", instance.allowSystemPinFallback);
        instance.shouldClearVaultAfterTooManyFailedAttempts = config.optBoolean("shouldClearVaultAfterTooManyFailedAttempts", instance.shouldClearVaultAfterTooManyFailedAttempts);
        instance.lockAfter = config.optInt("lockAfter", instance.lockAfter);
        instance.hideScreenOnBackground = config.optBoolean("hideScreenOnBackground", instance.hideScreenOnBackground);
        // Goofy place maybe but store the flag is shared prefs since the onResume/onPause handlers
        // don't seem to have access to the in memory vault
        activity.getApplicationContext()
                .getSharedPreferences("com.ionicframework.iv", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("hideScreen", instance.hideScreenOnBackground)
                .commit();
        instance.configured = true;
    }
}
