package com.example.devsocattendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Scanner extends AppCompatActivity {

    private static final String TAG = "Scanner";
    private CameraSource cameraSource;
    private CameraSourcePreview cameraSourcePreview;
    private GraphicOverlay graphicOverlay;

    private GestureDetector gestureDetector;

    private static int numBackPressed = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        Log.d(TAG, "onCreate: Started Scanner");

        cameraSourcePreview = findViewById(R.id.preview);
        graphicOverlay = findViewById(R.id.graphicOverlay);

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());

        Log.d(TAG, "onCreate: Created preview and overlay");

        //Check for camera permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        Log.d(TAG, "onCreate: Permission Code " + rc);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onCreate: Camera permission granted. Creating camera source");
            createCameraSource();
        } else {
            Log.d(TAG, "onCreate: No Permission For Accessing Camera");
            requestCameraPermission();
        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        //Checking if back is pressed 3 times and if true, close the app.
        numBackPressed++;
        if (numBackPressed == 3) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }
        Toast.makeText(Scanner.this, "Press Back " + (3 - numBackPressed) + " times to exit", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean c = gestureDetector.onTouchEvent(e);

        return c || super.onTouchEvent(e);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSourcePreview != null) {
            Log.d(TAG, "onDestroy: Releasing camera source");
            cameraSourcePreview.release();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraSourcePreview != null) {
            Log.d(TAG, "onPause: Pausing camera source");
            cameraSourcePreview.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Staring Camera Source");
        startCameraSource();
        numBackPressed = 0;
    }

    @SuppressLint("InLinedApi")
    private void createCameraSource() {
        Log.d(TAG, "createCameraSource: Creating camera source started");

        Context context = getApplicationContext();

        //Creating barcode detector with multiprocessor.
        BarcodeDetector detector = new BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        BarcodeTrackerFactory factory = new BarcodeTrackerFactory(graphicOverlay);
        detector.setProcessor(new MultiProcessor.Builder<>(factory).build());

        if (!detector.isOperational()) {
            Log.d(TAG, "createCameraSource: Detector is not operational");

            //Checking if device has low storage to update the libraries.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, "Device has Low Storage", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Device has Low Storage");
            }
        }

        //Building Camera Object.
        //Facing Back Camera
        //Auto-Focus Enabled
        //Resolution 1600x1024
        //Requested FPS: 15.0
        CameraSource.Builder builder = new CameraSource.Builder(context, detector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f);
//            builder = builder.setFocusMode(
//                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        cameraSource = builder.build();
        if (cameraSource != null) {
            Log.d(TAG, "createCameraSource: Camera Source Created");
        }
    }

    private void startCameraSource() throws SecurityException {
        Log.d(TAG, "startCameraSource: Beginning to start camera source");
        //Check For Google Api Availability.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Log.d(TAG, "startCameraSource: Google Play Not Available");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, code, 9001);
            dialog.show();
        }

        if (cameraSource != null) {
            try {
                Log.d(TAG, "startCameraSource: Starting Camera Source Preview");
                cameraSourcePreview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "startCameraSource: Unable to start camera source", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    private void requestCameraPermission() {
        Log.d(TAG, "requestCameraPermission: Starting to request camera permission.");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

//        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
//            Log.d(TAG, "requestCameraPermission: Launching request with code 2");
//            ActivityCompat.requestPermissions(this, permissions, 2);
//        }
        ActivityCompat.requestPermissions(this, permissions, 2);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: Checking the result of asking permission");

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onRequestPermissionsResult: Permission Granted For the Camera");
            Log.d(TAG, "onRequestPermissionsResult: Starting the Camera Source.");
            createCameraSource();
        } else {
//            cameraSource.release();
            //Return to the previous activity.
            finish();
        }
    }

    private boolean onTap(float x, float y) {
        try {
            BarcodeGraphic barcodeGraphic = graphicOverlay.getGraphics().get(0);
            Barcode barcode = barcodeGraphic.getBarcode();
            Log.d(TAG, "onTap: Extracted Barcode Value: " + barcode.displayValue);
            SharedPreferences preferences = getSharedPreferences("Login", MODE_PRIVATE);
            String postLink = barcode.displayValue + preferences.getString("email", null) + "/";
            Log.d(TAG, "onTap: Post Link: " + postLink);
            PostAttendance attendance = new PostAttendance(postLink);

//            Toast.makeText(getApplicationContext(), barcode.displayValue, Toast.LENGTH_LONG).show();
        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "onTap: Exception Caught...No Barcode Found");
        }
        return true;
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

}
