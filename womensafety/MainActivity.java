package com.example.womensafety;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private Button buttonStart, buttonStop;
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private MediaPlayer mediaPlayer; // For playing the siren sound

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonStart = findViewById(R.id.buttonStart);
        buttonStop = findViewById(R.id.buttonStop);

        // Check if emergency numbers are registered
        checkRegisteredNumbers();

        // Set click listeners for start and stop buttons
        buttonStart.setOnClickListener(v -> requestPermissionsAndStartService());
        buttonStop.setOnClickListener(v -> stopShakeDetectionService());
    }

    // Check if the emergency numbers are registered, and if not, redirect to the registration activity
    private void checkRegisteredNumbers() {
        SharedPreferences sharedPreferences = getSharedPreferences("PhoneNo", MODE_PRIVATE);
        String callNumber = sharedPreferences.getString("CALLNUM", null);
        String smsNumber1 = sharedPreferences.getString("SMSNUM1", null);
        String smsNumber2 = sharedPreferences.getString("SMSNUM2", null);

        if (callNumber == null || smsNumber1 == null || smsNumber2 == null) {
            Intent intent = new Intent(this, RegisterNo.class);
            startActivity(intent);
            finish(); // Close MainActivity to prevent returning to it
        }
    }

    // Request necessary permissions and start the shake detection service if permissions are granted
    private void requestPermissionsAndStartService() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS
        };

        // Check if all permissions are granted
        if (arePermissionsGranted(permissions)) {
            startShakeDetectionService();
        } else {
            // Request necessary permissions
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_CODE);
        }
    }

    // Check if all required permissions are granted
    private boolean arePermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Start the ShakeDetectionService
    private void startShakeDetectionService() {
        Intent serviceIntent = new Intent(this, ShakeDetectionService.class);
        startService(serviceIntent);
        Toast.makeText(this, "Shake detection service started. Shake your device to trigger.", Toast.LENGTH_SHORT).show();
        Log.d("MainActivity", "Shake detection service started.");
    }

    // Stop the ShakeDetectionService
    private void stopShakeDetectionService() {
        Intent serviceIntent = new Intent(this, ShakeDetectionService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Shake detection service stopped.", Toast.LENGTH_SHORT).show();
        Log.d("MainActivity", "Shake detection service stopped.");
    }

    // Handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (areAllPermissionsGranted(grantResults)) {
                // All permissions granted, start the service
                startShakeDetectionService();
            } else {
                Toast.makeText(this, "All permissions are required to start the service", Toast.LENGTH_SHORT).show();
                logDeniedPermissions(permissions, grantResults);
            }
            // Log current permission statuses
            logPermissions();
        }
    }

    // Check if all permissions were granted
    private boolean areAllPermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Log denied permissions for debugging
    private void logDeniedPermissions(String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Log.e("Permissions", permissions[i] + " not granted");
            }
        }
    }

    // Log current permission statuses for debugging purposes
    private void logPermissions() {
        for (String permission : new String[]{
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION
        }) {
            Log.d("PermissionCheck", permission + ": " + (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED));
        }
    }

    // Inflate the options menu for changing numbers
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        android.view.MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu); // Ensure menu_main.xml is inflated
        return true;
    }

    // Handle menu item selection for changing the registered numbers
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_change_number) {
            Intent intent = new Intent(MainActivity.this, RegisterNo.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
