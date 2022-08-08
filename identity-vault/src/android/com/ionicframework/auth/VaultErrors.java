package com.ionicframework.auth;

import org.json.JSONException;
import org.json.JSONObject;

public class VaultErrors {
    public static final int ERR_UNHANDLED = 0;
    public static final int ERR_VAULT_LOCKED = 1;
    public static final int ERR_VAULT_UNAVAILABLE = 2;
    public static final int ERR_INVALID_ARGUMENTS = 3;
    public static final int ERR_INVALIDATED_CREDENTIALS = 4;
    public static final int ERR_SECURITY_NOT_AVAILABLE = 5;
    public static final int ERR_AUTH_FAILED = 6;
    public static final int ERR_TOO_MANY_FAILED_ATTEMPTS = 7;
    public static final int ERR_USER_CANCELED_AUTH = 8;
    public static final int ERR_MISMATCHED_PASSCODE = 9;
    public static final int ERR_MISSING_PASSCODE = 10;
    public static final int ERR_PASSCODE_NOT_ENABLED = 11;
    public static final int ERR_KEY_NOT_FOUND = 12;
    public static final int ERR_BIOMETRICS_NOT_ENABLED = 13;

    public static JSONObject toJSON(VaultError e) {
        if (e instanceof VaultLockedError) {
            return VaultErrors.getJSON(ERR_VAULT_LOCKED, "Operation not allowed while vault locked.");
        } else if(e instanceof VaultUnavailableError) {
            return VaultErrors.getJSON(ERR_VAULT_UNAVAILABLE, "Vault Unavailable: Make sure you've configured the vault.");
        } else if(e instanceof InvalidArgumentsError) {
            String defaultErr = "Invalid Arguments Provided: ";
            String detail = e.getMessage();
            String err = detail == null ?  defaultErr : defaultErr + detail;
            return VaultErrors.getJSON(ERR_INVALID_ARGUMENTS, err);
        } else if(e instanceof InvalidatedCredentialsError) {
            return VaultErrors.getJSON(ERR_INVALIDATED_CREDENTIALS, "Credentials invalidated or expired. Vault cleared.");
        } else if(e instanceof SecurityNotAvailableError) {
            return VaultErrors.getJSON(ERR_SECURITY_NOT_AVAILABLE, "Biometric Security unavailable.");
        } else if(e instanceof AuthFailedError) {
            return VaultErrors.getJSON(ERR_AUTH_FAILED, "Failed authorization attempt");
        } else if(e instanceof TooManyFailedAttemptsError) {
            return VaultErrors.getJSON(ERR_TOO_MANY_FAILED_ATTEMPTS, "Too many failed attempts.");
        } else if(e instanceof UserCanceledAuthError) {
            return VaultErrors.getJSON(ERR_USER_CANCELED_AUTH, "User canceled auth attempt.");
        } else if(e instanceof MismatchedPasscodeError) {
            return VaultErrors.getJSON(ERR_MISMATCHED_PASSCODE, "Passcodes did not match.");
        } else if(e instanceof MissingPasscodeError) {
            return VaultErrors.getJSON(ERR_MISSING_PASSCODE, "Passcode not setup yet. You must call setPasscode prior to storing values if passcode is enabled");
        } else if(e instanceof PasscodeNotEnabledError) {
            return VaultErrors.getJSON(ERR_PASSCODE_NOT_ENABLED, "Passcode not enabled.");
        } else if(e instanceof KeyNotFoundError) {
            return VaultErrors.getJSON(ERR_KEY_NOT_FOUND, "Key Not Found");
        } else if(e instanceof BiometricsNotEnabled) {
            return VaultErrors.getJSON(ERR_BIOMETRICS_NOT_ENABLED, "Biometric auth is not enabled");
        } else {
            String defaultErr = "Unhandled Error: ";
            String detail = e.getMessage();
            String err = detail == null ?  defaultErr : defaultErr + detail;
            return VaultErrors.getJSON(ERR_UNHANDLED, err);
        }
    }

    private static JSONObject getJSON(int code, String msg) {
        JSONObject ret = new JSONObject();
        try {
            ret.put("code", code);
            ret.put("message", msg);
            return ret;
        } catch (JSONException e) {
            return ret;
        }
    }

}

class VaultError extends Throwable {
    VaultError(){}
    VaultError(String msg) {
        super(msg);
    }
}
class VaultLockedError extends VaultError {}
class VaultUnavailableError extends VaultError {}
class InvalidArgumentsError extends VaultError {
    InvalidArgumentsError(String msg) {
        super(msg);
    }
}
class InvalidatedCredentialsError extends VaultError {}
class SecurityNotAvailableError extends VaultError {}
class AuthFailedError extends VaultError {}
class TooManyFailedAttemptsError extends VaultError {}
class UserCanceledAuthError extends VaultError {}
class MismatchedPasscodeError extends VaultError {}
class MissingPasscodeError extends VaultError {}
class PasscodeNotEnabledError extends VaultError {}
class KeyNotFoundError extends VaultError {}
class BiometricsNotEnabled extends VaultError {}
