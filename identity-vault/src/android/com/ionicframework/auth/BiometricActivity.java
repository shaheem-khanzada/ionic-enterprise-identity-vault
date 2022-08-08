package com.ionicframework.auth;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricConstants;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.concurrent.Executor;

public class BiometricActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 2;
    private BiometricPrompt.PromptInfo mPromptInfo;
    private Boolean mIsAndroidQ = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q;

    private BiometricPrompt.AuthenticationCallback mAuthenticationCallback =
            new BiometricPrompt.AuthenticationCallback() {

                /**
                 * Called when authentication fails after multiple attempts.
                 */
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    onPromptError(errorCode, errString);
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    onPromptSuccess(result);
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(null);
        if (savedInstanceState != null) {
            return;
        }

        authenticate();
    }

    private void authenticate() {
        final Handler handler = new Handler(Looper.getMainLooper());
        Executor executor = handler::post;
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, mAuthenticationCallback);
        VaultAppConfig config = VaultAppConfig.getInstance();
        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
            .setTitle(config.promptTitle)
            .setSubtitle(config.promptSubtitle)
            .setDescription(config.promptDescription)
            .setDeviceCredentialAllowed(config.allowSystemPinFallback);

        if (!config.allowSystemPinFallback) {
            builder.setNegativeButtonText(config.promptNegativeButtonText);
        }

        if (config.allowSystemPinFallback && mIsAndroidQ) { // needs Q workaround
            builder
                .setDeviceCredentialAllowed(false)
                .setNegativeButtonText("Use PIN");
            mPromptInfo = builder.build();
            showAuthenticationScreen();
        } else {
            mPromptInfo = builder.build();
            biometricPrompt.authenticate(mPromptInfo);
        }
    }

    private void onPromptError(int errorCode, @NonNull CharSequence errString) {
        switch (errorCode)
        {
            case BiometricPrompt.ERROR_USER_CANCELED:
            case BiometricPrompt.ERROR_CANCELED:
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                finishWithError(new UserCanceledAuthError());
                break;
            case BiometricPrompt.ERROR_LOCKOUT:
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                finishWithError(new TooManyFailedAttemptsError());
                break;
            case BiometricConstants.ERROR_HW_NOT_PRESENT:
                finishWithError(new SecurityNotAvailableError());
                break;
            case BiometricPrompt.ERROR_NO_BIOMETRICS:
            case BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL:
                finishWithError(new BiometricsNotEnabled());
            default:
                finishWithError(new VaultError());
        }
    }

    private void onPromptSuccess(@NonNull BiometricPrompt.AuthenticationResult result) {
        finishWithSuccess();
    }

    // TODO: remove after fix https://issuetracker.google.com/issues/142740104
    private void showAuthenticationScreen() {
        KeyguardManager keyguardManager = ContextCompat
                .getSystemService(this, KeyguardManager.class);
        if (keyguardManager == null
                || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        if (keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager
                    .createConfirmDeviceCredentialIntent(mPromptInfo.getTitle(), mPromptInfo.getDescription());
            this.startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        } else {
            // Show a message that the user hasn't set up a lock screen.
            finishWithError(new BiometricsNotEnabled());
        }
    }

    // TODO: remove after fix https://issuetracker.google.com/issues/142740104
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == Activity.RESULT_OK) {
                finishWithSuccess();
            } else {
                finishWithError(new UserCanceledAuthError());
            }
        }
    }

    private void finishWithError(VaultError error) {
        finishWithError(VaultErrors.toJSON(error));
    }

    private void finishWithError(JSONObject error) {
        Intent data = new Intent();
        data.putExtra("error", error.toString());
        setResult(RESULT_CANCELED, data);
        finish();
    }

    private void finishWithSuccess() {
        setResult(RESULT_OK);
        finish();
    }
}
