package com.victjava.scales;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Kostya on 07.06.2015.
 */
public class UserRecoverableAuth extends Activity {
    static final int REQUEST_ACTIVITY_AUTH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if ("UserRecoverableAuthIOException".equalsIgnoreCase(getIntent().getAction())) {
                try {
                    startActivityForResult((Intent) getIntent().getExtras().get("request_authorization"), REQUEST_ACTIVITY_AUTH);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }


}
