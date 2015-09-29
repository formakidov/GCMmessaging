package com.example.formakidov.gcmmessaging;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class DemoActivity extends Activity implements View.OnClickListener{

    public static final String SENDER_ID = "137360378410";
    public static final String PROJECT_ID = "gcmproject-1c27c";
    public static final String SERVER_API_KEY = "AIzaSyDkthgnOCHPOxoEaHTz6VsxSdXdH3aKID0";

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static final String ACTION_NEW_MESSAGE = "message_from_server";

    static final String TAG = "GCMmessaging";

    EditText mDisplay;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    Context context;

    String regid;
    private EditText name1;
    private EditText value1;
    private EditText name2;
    private EditText value2;
    private EditText name3;
    private EditText value3;
    private EditText name4;
    private EditText value4;
    private EditText name5;
    private EditText value5;
    private ResponseReceiver responseReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        Button send = (Button) findViewById(R.id.send);
        Button clear = (Button) findViewById(R.id.clear);
        send.setOnClickListener(this);
        clear.setOnClickListener(this);

        name1 = (EditText) findViewById(R.id.name1);
        value1 = (EditText) findViewById(R.id.value1);
        name2 = (EditText) findViewById(R.id.name2);
        value2 = (EditText) findViewById(R.id.value2);
        name3 = (EditText) findViewById(R.id.name3);
        value3 = (EditText) findViewById(R.id.value3);
        name4 = (EditText) findViewById(R.id.name4);
        value4 = (EditText) findViewById(R.id.value4);
        name5 = (EditText) findViewById(R.id.name5);
        value5 = (EditText) findViewById(R.id.value5);

        mDisplay = (EditText) findViewById(R.id.display);

        context = getApplicationContext();

        responseReceiver = new ResponseReceiver();
        IntentFilter filter = new IntentFilter(ACTION_NEW_MESSAGE);
        registerReceiver(responseReceiver, filter);


        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            mDisplay.append("\nREGID:\n" + regid + "\n");
            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
            Toast.makeText(this, "No valid Google Play Services APK found.", Toast.LENGTH_LONG);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(responseReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check device for Play Services APK.
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                //TODO handle
                Log.i(TAG, "This device is not supported.");
                Toast.makeText(this, "This device is not supported.", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            Toast.makeText(this, "App version changed.", Toast.LENGTH_LONG);
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "\nREGID:\n" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "\nError:\n" + ex.getMessage();
                    // TODO perform exponential back-off
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }

    // Send an upstream message.
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send:
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        String msg = "";
                        try {
                            Bundle data = getFilledBundle();
//                            data.putString("my_message", "Hello World");
//                            data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
                            String id = Integer.toString(msgId.incrementAndGet());
                            gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
                            msg = "Success";
                        } catch (IOException ex) {
                            //TODO handle error
                            msg = "Error :" + ex.getMessage();
                        }
                        return msg;
                    }

                    @Override
                    protected void onPostExecute(String msg) {
                        mDisplay.append(msg + "\n");
                    }
                }.execute(null, null, null);
                break;
            case R.id.clear:
                mDisplay.setText("");
                break;
        }
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(DemoActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
      // TODO Your implementation here.
    }

    private Bundle getFilledBundle() {
        Bundle args = new Bundle();
        String s1 = name1.getText().toString();
        String s2 = name2.getText().toString();
        String s3 = name3.getText().toString();
        String s4 = name4.getText().toString();
        String s5 = name5.getText().toString();
        String v1 = value1.getText().toString();
        String v2 = value2.getText().toString();
        String v3 = value3.getText().toString();
        String v4 = value4.getText().toString();
        String v5 = value5.getText().toString();
        if (!s1.isEmpty()) args.putString(s1, v1);
        if (!s2.isEmpty()) args.putString(s2, v2);
        if (!s3.isEmpty()) args.putString(s3, v3);
        if (!s4.isEmpty()) args.putString(s4, v4);
        if (!s5.isEmpty()) args.putString(s5, v5);
        return args;
    }

    //messages from server
    private class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_NEW_MESSAGE)) {
                mDisplay.append("\nNEW MESSAGE:\n" + intent.getStringExtra("message"));
            }
        }
    }
}
