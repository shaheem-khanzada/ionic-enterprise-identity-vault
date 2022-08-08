package com.ionicframework.auth;

import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;


public class IonicNativeAuth extends CordovaPlugin {
  private static final String TAG = "IonicNativeAuthPlugin";
  private IdentityVault mCurrentVault = null;
  private boolean mLockedOutOfBiometrics = false;

  private static final int REQUEST_CODE_BIOMETRIC = 1;

  private CallbackContext mLastCallbackContext;


  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
    if (action.equals("setup")) {
      setup(args, callbackContext);
    } else if (action.equals("getConfig")) {
      getConfig(args, callbackContext);
    } else if (action.equals("close")) {
      close(args, callbackContext);
    } else if (action.equals("clear")) {
      clear(args, callbackContext);
    } else if (action.equals("isLocked")) {
      isLocked(args, callbackContext);
    } else if (action.equals("isLockedOutOfBiometrics")) {
      isLockedOutOfBiometrics(args, callbackContext);
    } else if (action.equals("isInUse")) {
      isInUse(args, callbackContext);
    } else if (action.equals("remainingAttempts")) {
      remainingAttempts(args, callbackContext);
    } else if (action.equals("getValue")) {
      getValue(args, callbackContext);
    } else if (action.equals("getKeys")) {
      getKeys(args, callbackContext);
    } else if (action.equals("storeValue")) {
      storeValue(args, callbackContext);
    } else if (action.equals("removeValue")) {
      removeValue(args, callbackContext);
    } else if (action.equals("getUsername")) {
      getUsername(args, callbackContext);
    } else if (action.equals("lock")) {
      lock(args, callbackContext);
    } else if (action.equals("getBiometricType")) {
      getBiometricType(args, callbackContext);
    } else if (action.equals("getAvailableHardware")) {
      getAvailableHardware(args, callbackContext);
    } else if (action.equals("isBiometricsAvailable")) {
      isBiometricsAvailable(args, callbackContext);
    } else if (action.equals("isBiometricsSupported")) {
      isBiometricsSupported(args, callbackContext);
    } else if (action.equals("setBiometricsEnabled")) {
      setBiometricsEnabled(args, callbackContext);
    } else if (action.equals("isBiometricsEnabled")) {
      isBiometricsEnabled(args, callbackContext);
    } else if (action.equals("setSecureStorageModeEnabled")) {
      setSecureStorageModeEnabled(args, callbackContext);
    } else if (action.equals("isSecureStorageModeEnabled")) {
      isSecureStorageModeEnabled(args, callbackContext);
    } else if (action.equals("isPasscodeEnabled")) {
      isPasscodeEnabled(args, callbackContext);
    } else if (action.equals("isPasscodeSetupNeeded")) {
      isPasscodeSetupNeeded(args, callbackContext);
    } else if (action.equals("setPasscodeEnabled")) {
      setPasscodeEnabled(args, callbackContext);
    } else if (action.equals("setPasscode")) {
      setPasscode(args, callbackContext);
    } else if (action.equals("unlock")) {
      unlock(args, callbackContext);
    }

    return true;
  }

  @Override
  public void onPause(boolean multitasking) {
    if (hideScreen()) {
      cordova.getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }
  }

  @Override
  public void onResume(boolean multitasking) {
    if (!usesGestureNavigation(cordova.getContext())) {
      cordova.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    } else if (hideScreen()) {
      cordova.getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }
  }

  private void setup(JSONArray args, final CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      mCurrentVault.addEventHandler(callbackContext);
      mCurrentVault.sendConfigEvent();
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void getConfig(JSONArray args, final CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.getConfig());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private  void close(JSONArray args, final CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      String handlerId = getPositionalArg(args, 1, "handlerId", String.class);
      mCurrentVault.removeEventHandler(handlerId);
      success(callbackContext);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void clear(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      mCurrentVault.clear();
    } catch (VaultError e) {
      error(callbackContext, e);
    }
    success(callbackContext);
  }

  private void isLocked(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.isLocked());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void isLockedOutOfBiometrics(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mLockedOutOfBiometrics);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void isInUse(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.isInUse());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void remainingAttempts(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.remainingAttempts());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void getValue(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      String key = getPositionalArg(args, 1, "key", String.class);
      Object val = mCurrentVault.getStoredValue(key);
      success(callbackContext, val, key);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void getKeys(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      JSONArray val = mCurrentVault.getKeys();
      success(callbackContext, val);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void storeValue(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      String key = getPositionalArg(args, 1, "key", String.class);
      Object val = args.opt(2);
      mCurrentVault.storeValue(key, val);
      success(callbackContext);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void removeValue(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      String key = getPositionalArg(args, 1, "key", String.class);
      mCurrentVault.removeValue(key);
      success(callbackContext);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void getUsername(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.getUsername());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void lock(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      mCurrentVault.lock(false);
      success(callbackContext);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void getBiometricType(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.getBiometricsType());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void getAvailableHardware(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.getAvailableHardware());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void isBiometricsAvailable(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.isBiometricsAvailable());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void isBiometricsSupported(JSONArray args, CallbackContext callbackContext) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      FingerprintManager fingerprintManager =
              (FingerprintManager) this.cordova.getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
      success(callbackContext, fingerprintManager != null && fingerprintManager.isHardwareDetected());
    } else {
      success(callbackContext, false);
    }
  }

  private void setBiometricsEnabled(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      boolean enabled = getPositionalArg(args, 1, "enabled", Boolean.class);
      mCurrentVault.setBiometricsEnabled(enabled);
      success(callbackContext);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void isBiometricsEnabled(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.isBiometricsEnabled());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void setSecureStorageModeEnabled(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      boolean enabled = getPositionalArg(args, 1, "enabled", Boolean.class);
      mCurrentVault.setSecureStorageModeEnabled(enabled);
      success(callbackContext);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void isSecureStorageModeEnabled(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.isSecureStorageModeEnabled());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void isPasscodeEnabled(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.isPasscodeEnabled());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void isPasscodeSetupNeeded(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      success(callbackContext, mCurrentVault.isPasscodeSetupNeeded());
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void setPasscodeEnabled(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      boolean enabled = getPositionalArg(args, 1, "enabled", Boolean.class);
      mCurrentVault.setPasscodeEnabled(enabled);
      success(callbackContext);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void setPasscode(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      String passcode = getPositionalArg(args, 1, "passcode", String.class, null);
      if (passcode != null) {
        mCurrentVault.setPasscode(passcode);
        success(callbackContext);
      } else {
        AuthPINDialog.PasscodeDialogCallback callback = new PasscodeCallback(
                mCurrentVault, callbackContext, true);
        mCurrentVault.setPasscode(callback);
      }
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  public class PasscodeCallback implements AuthPINDialog.PasscodeDialogCallback {
    private IdentityVault mVault;
    private CallbackContext callbackContext;
    private boolean setPasscode;

    private PasscodeCallback(IdentityVault vault, CallbackContext callbackContext, boolean setPasscode) {
      mVault = vault;
      this.callbackContext = callbackContext;
      this.setPasscode = setPasscode;
    }

    public void onPasscodeError(int err) {
      switch (err) {
        case AuthPINDialog.PASSCODE_MISMATCH:
          error(callbackContext, new MismatchedPasscodeError());
          return;
        case AuthPINDialog.USER_CANCELED:
          error(callbackContext, new UserCanceledAuthError());
          return;
        default:
          error(callbackContext, new VaultError("unknown error fetching passcode"));
      }
    }

    public void onPasscodeSuccess(String passcode) {
      if (passcode == null) {
        error(callbackContext, new VaultError("no passcode returned"));
      }
      try {
        if (setPasscode) {
          mVault.setPasscode(passcode);
          success(callbackContext);
          return;
        }
        mVault.unlock(passcode);
        success(callbackContext);
      } catch (VaultError e) {
        error(callbackContext, e);
      }
    }
  }

  private void unlock(JSONArray args, CallbackContext callbackContext) {
    try {
      setCurrentVaultFromArgs(args);
      boolean withPasscode = getPositionalArg(args, 1, "withPasscode", Boolean.class, false);

      if (withPasscode) {
        String passcode = getPositionalArg(args, 2, "passcode", String.class, null);
        if (passcode != null) {
          unlockWithPasscode(callbackContext, passcode);
        } else {
          unlockWithDialog(callbackContext);
        }
      } else {
        unlockWithBio(callbackContext);
      }
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void unlockWithPasscode(CallbackContext callbackContext, String passcode) {
    try {
      mCurrentVault.unlock(passcode);
      success(callbackContext);
    } catch (VaultError e) {
      error(callbackContext, e);
    }
  }

  private void unlockWithDialog(CallbackContext callbackContext) {
      try {
        AuthPINDialog.PasscodeDialogCallback callback = new PasscodeCallback(mCurrentVault, callbackContext, false);
        mCurrentVault.unlock(callback);
      } catch (VaultError e) {
        error(callbackContext, e);
      }
  }

  private void unlockWithBio(CallbackContext callbackContext) {
    mLastCallbackContext = callbackContext;
    mCurrentVault.doTheLifecycles = false;
    cordova.getActivity().runOnUiThread(() -> {
      Intent intent = new Intent(cordova.getActivity().getApplicationContext(), BiometricActivity.class);
      this.cordova.startActivityForResult(this, intent, REQUEST_CODE_BIOMETRIC);
    });
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);

    if (requestCode == REQUEST_CODE_BIOMETRIC) {
      onBiometricActivityResult(resultCode, intent);
    }
  }

  private void onBiometricActivityResult(int resultCode, Intent intent) {
    mCurrentVault.doTheLifecycles = true;
    mLockedOutOfBiometrics = false;

    if (resultCode == Activity.RESULT_OK) {
      try {
        mCurrentVault.forceUnlock();
        success(mLastCallbackContext);
      } catch (VaultError e) {
        error(mLastCallbackContext, e);
      }
    } else if (intent != null) {
      Bundle extras = intent.getExtras();
      if (extras != null) {
        try {
          JSONObject jsonError = new JSONObject(extras.getString("error", VaultErrors.toJSON(new VaultError()).toString()));
          int code = jsonError.getInt("code");
          if (code == VaultErrors.ERR_TOO_MANY_FAILED_ATTEMPTS) {
            mLockedOutOfBiometrics = true;

            // Biometrics lockout lasts for 30 seconds
            // @see https://developer.android.com/reference/android/hardware/biometrics/BiometricPrompt#BIOMETRIC_ERROR_LOCKOUT
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
              @Override
              public void run() {
                mLockedOutOfBiometrics = false;
              }
            }, 30 * 1000);
          }
          error(mLastCallbackContext, jsonError);
        } catch (JSONException e) {
          error(mLastCallbackContext, new VaultError());
        }
      } else {
        error(mLastCallbackContext, new VaultError());
      }
    } else {
      error(mLastCallbackContext, new UserCanceledAuthError());
    }
  }

  private <T extends Object> T getPositionalArg(JSONArray args, int index, String name, Class<T> type) throws InvalidArgumentsError {
    if (args.isNull(index)) {
      throw new InvalidArgumentsError(name + " missing");
    }

    Object o = args.opt(index);
    Class c = o.getClass();
    if (c != type) {
      throw new InvalidArgumentsError(name + " must be a " + type.getSimpleName());
    }
    return type.cast(o);
  }

  private <T extends Object> T getPositionalArg(JSONArray args, int index, String name, Class<T> type, T defaultTo) throws InvalidArgumentsError {
    try {
      return getPositionalArg(args, index, name, type);
    } catch (InvalidArgumentsError e) {
      if (e.getMessage().contains("missing")) {
        return defaultTo;
      }
      throw e;
    }
  }

  private void setCurrentVaultFromArgs(JSONArray args) throws VaultError {
    try {
      JSONObject options = args.getJSONObject(0);
      mCurrentVault = IdentityVault.getFromRegistryOrCreate(cordova.getActivity(), options);
    } catch (JSONException ex) {
      throw  new InvalidArgumentsError("missing vaultId or username");
    }
  }

  private void success(final CallbackContext callbackContext, final JSONObject data) {
    JSONObject ret = (data == null) ? new JSONObject() : data;
    PluginResult result = new PluginResult(PluginResult.Status.OK, ret);
    result.setKeepCallback(false);
    callbackContext.sendPluginResult(result);
  }

  private void success(final CallbackContext callbackContext, final JSONArray data) {
    JSONArray ret = (data == null) ? new JSONArray() : data;
    PluginResult result = new PluginResult(PluginResult.Status.OK, ret);
    result.setKeepCallback(false);
    callbackContext.sendPluginResult(result);
  }

  private void success(final CallbackContext callbackContext, boolean val) {
    PluginResult result = new PluginResult(PluginResult.Status.OK, val);
    result.setKeepCallback(false);
    callbackContext.sendPluginResult(result);
  }

  private void success(final CallbackContext callbackContext, Integer val) {
    PluginResult result = new PluginResult(PluginResult.Status.OK, val);
    result.setKeepCallback(false);
    callbackContext.sendPluginResult(result);
  }

  private void success(final CallbackContext callbackContext, String val) {
    PluginResult result = new PluginResult(PluginResult.Status.OK, val);
    result.setKeepCallback(false);
    callbackContext.sendPluginResult(result);
  }

  private void success(final CallbackContext callbackContext, Object val, String key) {
    JSONObject ret = new JSONObject();
    try {
      if (val == null) {
        success(callbackContext);
      } else if (val instanceof String) {
        String typedVal = (String)val;
        ret.put(key, typedVal);
      } else if (val instanceof Integer) {
        Integer typedVal = (Integer)val;
        ret.put(key, typedVal);
      } else if (val instanceof Long) {
        Long typedVal = (Long)val;
        ret.put(key, typedVal);
      } else if (val instanceof Float) {
        Float typedVal = (Float)val;
        ret.put(key, typedVal);
      } else if (val instanceof Double) {
        Double typedVal = (Double)val;
        ret.put(key, typedVal);
      } else if (val instanceof Boolean) {
        Boolean typedVal = (Boolean)val;
        ret.put(key, typedVal);
      } else if (val instanceof JSONArray) {
        JSONArray typedVal = (JSONArray)val;
        ret.put(key, typedVal);
      } else if (val instanceof JSONObject) {
        JSONObject typedVal = (JSONObject)val;
        ret.put(key, typedVal);
      } else {
        error(callbackContext, new VaultError("Invalid return type of:" + val.getClass().getSimpleName()));
      }
      success(callbackContext, ret);
    } catch (JSONException e) {
      error(callbackContext, new VaultError("error creating return json"));
    }
  }

  private void success(final CallbackContext callbackContext) {
    String nullString = null;
    // NOTE: use null string to get the correct PluginResult call signature
    PluginResult result = new PluginResult(PluginResult.Status.OK, nullString);
    result.setKeepCallback(false);
    callbackContext.sendPluginResult(result);
  }

  private void error(final CallbackContext callbackContext, VaultError error) {
    JSONObject jsonError = VaultErrors.toJSON(error);
    error(callbackContext, jsonError);
  }

  private void error(final CallbackContext callbackContext, JSONObject error) {
    handleVaultError(error);
    PluginResult result = new PluginResult(PluginResult.Status.ERROR, error);
    result.setKeepCallback(false);
    callbackContext.sendPluginResult(result);
  }

  private void handleVaultError(JSONObject error) {
    try {
      int code = error.getInt("code");

      if (code == VaultErrors.ERR_TOO_MANY_FAILED_ATTEMPTS) {
        if (mCurrentVault.config.appConfig.shouldClearVaultAfterTooManyFailedAttempts) {
          mCurrentVault.clear();
        }
      }
    } catch (JSONException | VaultError e) {
      // ignore
    }
  }

  private boolean hideScreen() {
    return cordova.getActivity()
            .getApplicationContext()
            .getSharedPreferences("com.ionicframework.iv", Context.MODE_PRIVATE)
            .getBoolean("hideScreen", false);
  }
  private boolean usesGestureNavigation(Context context) {
    Resources resources = context.getResources();
    int resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android");
    return resourceId > 0 && resources.getInteger(resourceId) == 2;
  }
}
