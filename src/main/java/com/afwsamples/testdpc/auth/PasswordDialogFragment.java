/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.afwsamples.testdpc.auth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afwsamples.testdpc.R;

/**
 * Dialog fragment for password authentication and creation.
 */
public class PasswordDialogFragment extends DialogFragment {

    public static final String TAG = "PasswordDialogFragment";

    private static final String ARG_MODE = "mode";
    private static final String ARG_REQUIRE_OLD_PASSWORD = "require_old_password";

    public enum Mode {
        CREATE,         // First launch - create password
        AUTHENTICATE,   // Subsequent launches - enter password
        CHANGE          // Change password - requires old password first
    }

    private Mode mMode;
    private boolean mRequireOldPassword;
    private PasswordDialogListener mListener;
    private EditText mPasswordInput;
    private EditText mConfirmPasswordInput;
    private EditText mOldPasswordInput;
    private TextView mErrorText;
    private View mDialogView;

    /**
     * Listener interface for password dialog callbacks.
     */
    public interface PasswordDialogListener {
        void onPasswordCreated(String password);
        void onPasswordAuthenticated();
        void onPasswordChanged(String newPassword);
        void onCancelled();
    }

    /**
     * Creates a new PasswordDialogFragment in the specified mode.
     */
    public static PasswordDialogFragment newInstance(Mode mode) {
        return newInstance(mode, false);
    }

    /**
     * Creates a new PasswordDialogFragment with option to require old password.
     */
    public static PasswordDialogFragment newInstance(Mode mode, boolean requireOldPassword) {
        PasswordDialogFragment fragment = new PasswordDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode.name());
        args.putBoolean(ARG_REQUIRE_OLD_PASSWORD, requireOldPassword);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(PasswordDialogListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMode = Mode.valueOf(getArguments().getString(ARG_MODE));
            mRequireOldPassword = getArguments().getBoolean(ARG_REQUIRE_OLD_PASSWORD, false);
        }
        // Prevent dialog from being dismissed by back button or outside touch
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        mDialogView = inflater.inflate(R.layout.dialog_password, null);

        mPasswordInput = mDialogView.findViewById(R.id.password_input);
        mConfirmPasswordInput = mDialogView.findViewById(R.id.confirm_password_input);
        mOldPasswordInput = mDialogView.findViewById(R.id.old_password_input);
        mErrorText = mDialogView.findViewById(R.id.error_text);

        // Set up password input type
        mPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mConfirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mConfirmPasswordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mOldPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mOldPasswordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

        // Configure UI based on mode
        int titleResId = configureForMode();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(titleResId)
                .setView(mDialogView)
                .setPositiveButton(R.string.ok, null) // Set listener later to prevent auto-dismiss
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    if (mListener != null) {
                        mListener.onCancelled();
                    }
                });

        // Prevent cancellation for CREATE and AUTHENTICATE modes
        if (mMode == Mode.CREATE || mMode == Mode.AUTHENTICATE) {
            builder.setNegativeButton(null, null); // No cancel button
        }

        AlertDialog dialog = builder.create();
        
        // Override positive button click to prevent auto-dismiss on validation error
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> onPositiveButtonClick());
        });

        return dialog;
    }

    private int configureForMode() {
        switch (mMode) {
            case CREATE:
                mOldPasswordInput.setVisibility(View.GONE);
                mConfirmPasswordInput.setVisibility(View.VISIBLE);
                return R.string.create_password_title;
            case AUTHENTICATE:
                mOldPasswordInput.setVisibility(View.GONE);
                mConfirmPasswordInput.setVisibility(View.GONE);
                return R.string.enter_password_title;
            case CHANGE:
                if (mRequireOldPassword) {
                    mOldPasswordInput.setVisibility(View.VISIBLE);
                    mConfirmPasswordInput.setVisibility(View.VISIBLE);
                } else {
                    mOldPasswordInput.setVisibility(View.GONE);
                    mConfirmPasswordInput.setVisibility(View.VISIBLE);
                }
                return R.string.change_password_title;
            default:
                return R.string.create_password_title;
        }
    }

    private void onPositiveButtonClick() {
        String password = mPasswordInput.getText().toString().trim();

        switch (mMode) {
            case CREATE:
                handleCreateMode(password);
                break;
            case AUTHENTICATE:
                handleAuthenticateMode(password);
                break;
            case CHANGE:
                handleChangeMode(password);
                break;
        }
    }

    private void handleCreateMode(String password) {
        String confirmPassword = mConfirmPasswordInput.getText().toString().trim();

        if (!validateAlphaNumeric(password)) {
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError(getString(R.string.passwords_do_not_match));
            return;
        }

        if (mListener != null) {
            mListener.onPasswordCreated(password);
        }
        dismiss();
    }

    private void handleAuthenticateMode(String password) {
        PasswordManager passwordManager = new PasswordManager(getActivity());

        if (!passwordManager.validatePassword(password)) {
            showError(getString(R.string.incorrect_password));
            mPasswordInput.setText("");
            return;
        }

        passwordManager.setAuthenticated(true);
        if (mListener != null) {
            mListener.onPasswordAuthenticated();
        }
        dismiss();
    }

    private void handleChangeMode(String newPassword) {
        PasswordManager passwordManager = new PasswordManager(getActivity());

        if (mRequireOldPassword) {
            String oldPassword = mOldPasswordInput.getText().toString().trim();
            if (!passwordManager.validatePassword(oldPassword)) {
                showError(getString(R.string.incorrect_old_password));
                return;
            }
        }

        if (!validateAlphaNumeric(newPassword)) {
            return;
        }

        String confirmPassword = mConfirmPasswordInput.getText().toString().trim();
        if (!newPassword.equals(confirmPassword)) {
            showError(getString(R.string.passwords_do_not_match));
            return;
        }

        passwordManager.setPassword(newPassword);
        if (mListener != null) {
            mListener.onPasswordChanged(newPassword);
        }
        dismiss();
    }

    private boolean validateAlphaNumeric(String password) {
        if (!PasswordManager.isValidAlphaNumericPassword(password)) {
            showError(getString(R.string.password_must_be_alphanumeric));
            return false;
        }
        return true;
    }

    private void showError(String message) {
        mErrorText.setText(message);
        mErrorText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mListener != null) {
            mListener.onCancelled();
        }
    }
}
