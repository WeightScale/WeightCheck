/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.victjava.scales;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;
import com.victjava.scales.service.ServiceProcessTask;

import java.util.Collections;
import java.util.HashMap;

/**
 * Created by Kostya on 18.12.2014.
 */
public class ActivityGoogleDrivePreference extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    Preferences preferences;
    static final int REQUEST_ACCOUNT_PICKER = 1;
    static final int REQUEST_ACTIVITY_AUTH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.goole_drive);
        HashMap<String, String> hashMap = (HashMap<String, String>) getIntent().getSerializableExtra("map");

        try {
            if ("UserRecoverableAuthIOException".equalsIgnoreCase(getIntent().getAction())) {
                try {
                    startActivityForResult((Intent) getIntent().getExtras().get("request_authorization"), REQUEST_ACTIVITY_AUTH);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        //preferences = new Preferences(getSharedPreferences(getResources().getString(R.string.pref_settings), Context.MODE_PRIVATE));
        Preference name = findPreference(getString(R.string.key_account_name));

        if (name != null) {
            try {
                name.setSummary(hashMap.get("502"));
            } catch (Exception e) {
                name.setSummary(preferences.read(getString(R.string.key_account_name), ""));
            }
            name.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //*Установить Сервисы Google play*//*
                    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(ActivityGoogleDrivePreference.this, Collections.singleton(DriveScopes.DRIVE_FILE));
                    //GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(ActivityGoogleDrivePreference.this, Arrays.asList(DriveScopes.DRIVE_FILE));
                    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
                    return false;
                }
            });
        }

        name = findPreference(getString(R.string.key_folder_id));
        if (name != null) {
            try {
                name.setSummary(hashMap.get("511"));
            } catch (Exception e) {
                name.setSummary("folder id: " + preferences.read(getString(R.string.key_folder_id), "NULL"));
            }
            name.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    preference.setSummary("folder id: " + o);
                    preferences.write(preference.getKey(), o.toString());
                    startService(new Intent(ActivityGoogleDrivePreference.this, ServiceProcessTask.class));
                    return false;
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        preferences.write(getString(R.string.key_account_name), accountName);
                        findPreference(getString(R.string.key_account_name)).setSummary(preferences.read(getString(R.string.key_account_name), ""));
                    }
                }
                break;
            case REQUEST_ACTIVITY_AUTH:
                if (resultCode == RESULT_OK) {
                    startService(new Intent(this, ServiceProcessTask.class));
                    onBackPressed();
                }
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
