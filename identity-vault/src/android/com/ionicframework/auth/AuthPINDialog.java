package com.ionicframework.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AuthPINDialog {
    public static final int USER_CANCELED = 100;
    public static final int PASSCODE_MISMATCH = 101;

    private static final int STATE_VERIFY_PIN = 1;
    private static final int STATE_SET_PIN    = 2;
    private static final int STATE_CONF_PIN   = 3;

    private int state = STATE_VERIFY_PIN;
    private String setPIN = "";
    private boolean verifyOnly;
    private Activity activity;
    private PasscodeDialogCallback callback;

    public interface PasscodeDialogCallback {
        void onPasscodeSuccess(String passcode);
        void onPasscodeError(int errorCode);
    }

    public AuthPINDialog(Activity activity, boolean verify, PasscodeDialogCallback cb) {
        this.activity = activity;
        verifyOnly = verify;
        callback = cb;

    }

    private String getStringFromID(String string_id) {
        int id = activity.getResources()
                .getIdentifier(string_id, "string", activity.getPackageName());
        return  activity.getString(id);
    }

    public void display() {
        final String message = getStringFromID("IV_enter_PIN");
        final String titleVerify = getStringFromID("IV_verifyPIN");
        final String titleSet = getStringFromID("IV_setPIN");
        final String titleConfirm = getStringFromID("IV_confirmPIN");
        final String button1 = getStringFromID("IV_cancel");
        final String button2 = getStringFromID("IV_ok");

        final EditText promptInput =  new EditText(activity);
        promptInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        promptInput.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            promptInput.setLetterSpacing((float) 1.8);
        }
        promptInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

        Runnable runnable = new Runnable() {
            public void run() {
                AlertDialog.Builder dlg = new AlertDialog.Builder(activity);
                if (verifyOnly) {
                    state = STATE_VERIFY_PIN;
                    dlg.setTitle(titleVerify);
                } else  {
                    state = STATE_SET_PIN;
                    dlg.setTitle(titleSet);
                }
                dlg.setMessage(message)
                        .setCancelable(true)
                        .setView(promptInput)
                        .setPositiveButton(button2, null)
                        .setNegativeButton(button1, null);



                dlg.setOnCancelListener(new AlertDialog.OnCancelListener() {
                    public void onCancel(DialogInterface dialog){
                        dialog.dismiss();
                        callback.onPasscodeError(USER_CANCELED);
                    }
                });
                final AlertDialog dialog = dlg.create();

                // We want to keep showing the dialogs in case validations fail or move to another state
                // Use setOnShowListener

                dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                    @Override
                    public void onShow(DialogInterface dialogInterface) {

                        // Cancel button
                        Button buttonCancel = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                        buttonCancel.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View view) {
                                dialog.dismiss();
                                String result =  promptInput.getText().toString().trim();
                                callback.onPasscodeError(USER_CANCELED);
                            }
                        });

                        // OK button
                        Button buttonOK = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                        buttonOK.setOnClickListener(new View.OnClickListener() {
                            @RequiresApi(api = Build.VERSION_CODES.M)
                            public void onClick(View view) {
                                String result =  promptInput.getText().toString().trim();
                                if (state == STATE_VERIFY_PIN) {
                                    callback.onPasscodeSuccess(result);
                                    dialog.dismiss();
                                    return;
                                } else if (state == STATE_SET_PIN) {
                                    setPIN = result;
                                    state = STATE_CONF_PIN;
                                    promptInput.setText("");
                                    dialog.setTitle(titleConfirm);
                                    return;
                                } else if (state == STATE_CONF_PIN) {
                                    if (!result.equals(setPIN)) {
                                        Toast.makeText(activity.getApplication(),
                                                "ERR: PIN didn't match", Toast.LENGTH_LONG);
                                        callback.onPasscodeError(PASSCODE_MISMATCH);
                                        dialog.dismiss();
                                        return;
                                    }
                                }
                                dialog.dismiss();
                                callback.onPasscodeSuccess(result);
                            }
                        });
                    }
                });

                dialog.show();
            };
        };
        activity.runOnUiThread(runnable);
    }
}
