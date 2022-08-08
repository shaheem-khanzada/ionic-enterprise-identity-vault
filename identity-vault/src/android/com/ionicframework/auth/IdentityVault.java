package com.ionicframework.auth;

import android.app.Activity;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import androidx.biometric.BiometricManager;

import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class IdentityVault implements LifecycleObserver {

    final public static int UNLOCK_VAULT_BIO = 100;
    private IonicCombinedVault mVault;
    private Context context;
    public final Activity activity;
    private final VaultDescriptor descriptor;
    public AuthConfig config;
    private Date backgroundStart;
    final private String TAG = "IdentityVault";
    private HashMap<String, CallbackContext> handlers = new HashMap<>();
    private static HashMap<String, IdentityVault> vaultRegistry = new HashMap<>();

    /**
     * Should this class run the timer in the lifecycle hooks that locks the vault with wasTimeout: true after five seconds of the activity being in the background.
     */
    public boolean doTheLifecycles = true;

    IdentityVault(Activity activity, JSONObject options) throws VaultError {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        descriptor = new VaultDescriptor(options);
        config = new AuthConfig(options, activity, descriptor);
        mVault = new IonicCombinedVault(context, descriptor.getUniqueId(), this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    public static IdentityVault getFromRegistryOrCreate(Activity activity, JSONObject options) throws VaultError {
        VaultDescriptor descriptor = new VaultDescriptor(options);
        IdentityVault vault = vaultRegistry.get(descriptor.getUniqueId());
        if (vault == null) {
            vault = new IdentityVault(activity, options);
            vaultRegistry.put(descriptor.getUniqueId(), vault);
        }
        return vault;
    }

    public static void removeFromRegistry(String uniqueId) {
        vaultRegistry.remove(uniqueId);
    }

    public boolean isBiometricsAvailable() {
        if (!VaultFactory.canUseKeychainAuthentication(context)) {
            Log.d(TAG, "Keychain Auth Unavailable: Biometrics Unavailable");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            BiometricManager biometricManager = BiometricManager.from(this.activity.getApplicationContext());
            // Device supports fingerprint authentication hardware & user has enrolled fingerprints
            return biometricManager != null && biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
        }
        Log.d(TAG, "Android Version less than M: Biometrics Unavailable");
        return false;
    }

    public String getBiometricsType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isBiometricsAvailable()) {
            FingerprintManager fingerprintManager = (FingerprintManager) this.activity.getSystemService(Context.FINGERPRINT_SERVICE);
            if (fingerprintManager != null && fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints()) {
                return "touchID";
            } else {
                return "faceID";
            }
        }
        return "none";
    }

    public JSONArray getAvailableHardware() {
        final String FINGER = "fingerprint";
        final String IRIS = "iris";
        final String FACE = "face";

        JSONArray hardware = new JSONArray();
        PackageManager packageManager = this.activity.getPackageManager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                hardware.put(FINGER);
        }

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)) {
            hardware.put(IRIS);
        } else {
            // check if Samsung's Iris service is present
            try {
                PackageInfo irisSamsung = packageManager.getPackageInfo("com.samsung.android.server.iris", PackageManager.GET_META_DATA);
                hardware.put(IRIS);
            } catch (PackageManager.NameNotFoundException e) {
                // do nada
            }
        }

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)) {
            hardware.put(FACE);
        } else {
            // check if Samsung's Face service is present
            try {
                PackageInfo faceSamsung = packageManager.getPackageInfo("com.samsung.android.bio.face.service", PackageManager.GET_META_DATA);
                hardware.put(FACE);
            } catch (PackageManager.NameNotFoundException e) {
                // do nada
            }
        }

        return hardware;
    }

    public void clear() throws VaultError {
        mVault.clear();
    }

    public JSONObject getConfig() throws VaultError {
        JSONObject conf = config.toJSONObject();
        try {
            conf.put("isBiometricsEnabled", isBiometricsEnabled());
            conf.put("isPasscodeEnabled", isPasscodeEnabled());
            conf.put("isPasscodeSetupNeeded", isPasscodeSetupNeeded());
            conf.put("isSecureStorageModeEnabled", isSecureStorageModeEnabled());
        } catch (JSONException e) {
            throw new VaultError("Error converting config to JSON");
        }
        return conf;
    }

    public boolean isPasscodeSetupNeeded() {
       return mVault.needsUserPasswordSetup();
    }

    public boolean isBiometricsEnabled() {
        return mVault.isBiometricsEnabled();
    }

    public boolean isPasscodeEnabled() {
        return mVault.isPasscodeEnabled();
    }

    public void lock(boolean wasTimeout) {
        boolean wasLocked = isLocked();
        mVault.lock();
        if (!wasLocked) {
            try {
                JSONObject data = new JSONObject();
                data.put("timeout", wasTimeout);
                data.put("saved", mVault.isInUse());
                sendEvent("lock", data);
            } catch (JSONException e) {
                // pass
            } catch (VaultError e) {
                // pass
            }
        }
    }

    public boolean isLocked() {
        return mVault.isLocked();
    }

    public boolean isInUse() {
        return mVault.isInUse();
    }

    public int remainingAttempts() {
        return mVault.getRemainingAttempts();
    }

    public Object getStoredValue(String key) throws VaultError {
        return mVault.getStoredValue(key);
    }

    public JSONArray getKeys() throws VaultError {
        return mVault.getKeys();
    }

    public void storeValue(String key, Object value) throws VaultError {
        mVault.storeValue(key, value);
    }

    public void removeValue(String key) throws VaultError {
        mVault.removeValue(key);
    }

    public String getUsername() {
        return config.descriptor.username;
    }

    public void setBiometricsEnabled(boolean enabled) throws VaultError {
        if (!isBiometricsAvailable() && enabled) { throw new SecurityNotAvailableError(); }
        mVault.setBiometricsEnabled(enabled);
        sendConfigEvent();
    }

    public void setPasscodeEnabled(boolean enabled) throws VaultError {
        mVault.setPasscodeEnabled(enabled);
        sendConfigEvent();
    }

    public void setSecureStorageModeEnabled(boolean enabled) throws VaultError {
        mVault.setSecureStorageModeEnabled(enabled);
        sendConfigEvent();
    }

    public boolean isSecureStorageModeEnabled() {
        return mVault.isSecureStorageModeEnabled();
    }

    public void setPasscode(String passcode) throws VaultError {
        if (!isPasscodeEnabled()) { throw new PasscodeNotEnabledError(); }
        boolean wasNeeded = isPasscodeSetupNeeded();
        mVault.setPasscode(passcode);
        if (wasNeeded) {
            sendConfigEvent();
        }
    }

    public void setPasscode(AuthPINDialog.PasscodeDialogCallback callback) throws VaultError {
        if (!isPasscodeEnabled()) { throw new PasscodeNotEnabledError(); }
        AuthPINDialog dialog = new AuthPINDialog(activity, false, callback);
        dialog.display();
    }

    public void unlock(String passcode) throws VaultError {
        if (!isPasscodeEnabled()) { throw new PasscodeNotEnabledError(); }
        if (!mVault.isLocked()) { return; }
        mVault.unlock(passcode);
        sendEvent("unlock", getConfig());
    }

    public void unlock(AuthPINDialog.PasscodeDialogCallback callback)  throws VaultError {
        if (!isPasscodeEnabled()) { throw new PasscodeNotEnabledError(); }
        AuthPINDialog dialog = new AuthPINDialog(activity, true, callback);
        dialog.display();
    }

    public void forceUnlock() throws VaultError {
        if (!isBiometricsEnabled()) { throw new BiometricsNotEnabled(); }
        mVault.unlock();
        sendEvent("unlock", getConfig());
    }

    public void addEventHandler(CallbackContext callbackContext) {
        handlers.put(callbackContext.getCallbackId(), callbackContext);
    }

    public void removeEventHandler(String handlerId) {
        handlers.remove(handlerId);
    }

    private Iterator<CallbackContext> getHandlers() {
        return handlers.values().iterator();
    }

    public void sendConfigEvent() throws VaultError {
       sendEvent("config", getConfig());
    }

    private void sendEvent(String eventName, JSONObject data) throws VaultError {
        try {
            Iterator<CallbackContext> it = getHandlers();
            while (it.hasNext()) {
                CallbackContext c = it.next();
                JSONObject j = new JSONObject();
                j.put("event", eventName);
                j.put("data", data);
                j.put("handlerId", c.getCallbackId());
                PluginResult result = new PluginResult(PluginResult.Status.OK, j);
                result.setKeepCallback(true);
                c.sendPluginResult(result);
            }
        } catch (JSONException e) {
            throw new VaultError("Error converting event to JSON");
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onMoveToForeground() {
        // app moved to foreground
        if (backgroundStart != null) {
            Date now = new Date();
            long diff = now.getTime() - backgroundStart.getTime();

            if (config.appConfig.lockAfter > 0 && diff > config.appConfig.lockAfter) {
                lock(true);
            }

            backgroundStart = null;
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onMoveToBackground() {
        if (doTheLifecycles) {
            // app moved to background
            backgroundStart = new Date();
        }
    }
}
