package com.syncmedia.sdk.demo;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.acr.syncmedia.ResultDeliveryType;
import com.acr.syncmedia.SMClient;
import com.acr.syncmedia.SMConfig;
import com.acr.syncmedia.SMEventsListener;
import com.acr.syncmedia.SMState;
import com.acr.utils.SMException;
import com.acr.utils.SMLogger;
import com.acr.utils.SMMetaData;

import java.util.UUID;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private SMClient mClient;

    private static final int REQUEST_PERMISSION_CODE = 1;

    private static final String[] PERMISSIONS_ARRAY = {
            Manifest.permission.RECORD_AUDIO
    };

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.startStpBtn);
        textView.setOnClickListener(v -> toggleStartStop());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int result : grantResults) {
            if (result != PERMISSION_GRANTED) {
                return;
            }
        }

        startClient();
    }

    private void toggleStartStop() {
        if (mClient == null) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_ARRAY,
                    REQUEST_PERMISSION_CODE);
        } else {
            cancel();
        }
    }

    private void startClient() {
        textView.setText(R.string.stop);

        if (mClient != null) {
            return;
        }

        int deliveryType = ResultDeliveryType.both;

        try {
            mClient =
                    new SMConfig.Builder()
                            .setCredentials(getString(R.string.access_key), getString(R.string.access_secret))
                            .setIdentifier(UUID.randomUUID().toString())
                            .setContext(this)
                            .setResultDeliveryType(ResultDeliveryType.both) //set to both callback & local
                            .setListener(new SMEventsListener() {
                                @Override
                                public void onSMStateChanged(@NonNull SMClient client, @SMState String state) {
                                    Log.d(TAG, "onSMStateChanged: " + state);
                                }

                                @Override
                                public void onResult(String acrId, long eventTs) {
                                    super.onResult(acrId, eventTs);
                                    Log.d(TAG, "onResult: " + acrId + ", eventTs: " + eventTs);
                                }
                            })
                            .setLogger(new SMLogger(true))
                            .build();
        } catch (SMException e) {
            SMLogger.e(TAG, "startClient", e);
        }

        mClient.setMetaData(new SMMetaData.Builder()
                .putVal("version", BuildConfig.VERSION_NAME)
                .putVal("utc_timestamp", System.currentTimeMillis())
                .putNestedMeta("location",
                        new SMMetaData.Builder()
                                .putVal("latitude", "latitude")
                                .putVal("longitude", "longitude"))
                .putNestedMeta("device",
                        new SMMetaData.Builder()
                                .putVal("brand", android.os.Build.MANUFACTURER)
                                .putVal("model", android.os.Build.MODEL)
                                .putVal("product", android.os.Build.PRODUCT)
                                .putVal("os", android.os.Build.VERSION.SDK_INT))
                .build());
    }

    protected void cancel() {
        if (this.mClient != null) {
            this.mClient.release();
            this.mClient = null;
        }

        textView.setText(R.string.start);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("MainActivity", "release");
        cancel();
    }
}