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

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Manages app-level password authentication.
 * Passwords are stored as SHA-256 hashes using device-protected storage.
 */
public class PasswordManager {

    private static final String PREFS_NAME = "app_auth_prefs";
    private static final String KEY_PASSWORD_HASH = "password_hash";
    private static boolean isAuthenticated = false;

    private final SharedPreferences mPrefs;

    public PasswordManager(Context context) {
        // Use device-protected storage for password preferences
        Context deviceProtectedContext = context.createDeviceProtectedStorageContext();
        mPrefs = deviceProtectedContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Checks if a password has been set.
     */
    public boolean hasPassword() {
        return mPrefs.contains(KEY_PASSWORD_HASH);
    }

    /**
     * Sets a new password. Replaces any existing password.
     */
    public void setPassword(String password) {
        String hashedPassword = hashPassword(password);
        mPrefs.edit()
                .putString(KEY_PASSWORD_HASH, hashedPassword)
                .apply();
    }

    /**
     * Validates a password against the stored hash.
     */
    public boolean validatePassword(String password) {
        String storedHash = mPrefs.getString(KEY_PASSWORD_HASH, null);
        if (storedHash == null) {
            return false;
        }
        String inputHash = hashPassword(password);
        return storedHash.equals(inputHash);
    }

    /**
     * Validates that a password is alpha-numeric.
     * Accepts letters (a-z, A-Z) and digits (0-9) only.
     */
    public static boolean isValidAlphaNumericPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return password.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * Sets the authentication state for the current session.
     */
    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    /**
     * Checks if user is currently authenticated in this session.
     */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
