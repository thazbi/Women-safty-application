package com.example.womensafety;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class ShakeDetectionService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 15.0f;
    private float lastX, lastY, lastZ;
    private long lastTime;
    private FusedLocationProviderClient fusedLocationClient;
    private MediaPlayer mediaPlayer;

    private static final String CHANNEL_ID = "ShakeDetectionServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        startForegroundService(); // Start service as foreground
    }

    private void startForegroundService() {
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Women Safety")
                .setContentText("Shake detection service is running...")
                .setSmallIcon(R.drawable.ic_service_icon)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Shake Detection Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastTime) > 100) {
            float deltaX = event.values[0] - lastX;
            float deltaY = event.values[1] - lastY;
            float deltaZ = event.values[2] - lastZ;

            lastX = event.values[0];
            lastY = event.values[1];
            lastZ = event.values[2];
            lastTime = currentTime;

            float shakeMagnitude = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
            if (shakeMagnitude > SHAKE_THRESHOLD) {
                sensorManager.unregisterListener(this);
                initiateSirenThenCallAndSms();
            }
        }
    }

    // Method to handle siren task first, then call, and SMS
    private void initiateSirenThenCallAndSms() {
        initiateSirenTask();

        // Wait for siren to complete, then start the rest
        new Handler().postDelayed(() -> {
            Log.d("ShakeDetection", "Siren task completed. Starting remaining tasks.");
            makeEmergencyCallThenSms();
        }, 10000); // Adjust delay based on siren duration (10 seconds in this example)
    }

    // Mock siren task, you can add the actual implementation here
    private void initiateSirenTask() {
        // Play sound from raw resource
        mediaPlayer = MediaPlayer.create(ShakeDetectionService.this, R.raw.siren_sound); // Use service context
        if (mediaPlayer != null) {
            mediaPlayer.start(); // Play the sound
            Toast.makeText(this, "Siren task initiated", Toast.LENGTH_SHORT).show();
            Log.d("ShakeDetection", "Siren task started.");

            // Release media player after completion to free resources
            mediaPlayer.setOnCompletionListener(mp -> {
                mediaPlayer.release();
                mediaPlayer = null;
            });
        } else {
            Log.e("ShakeDetection", "Failed to initialize MediaPlayer for siren sound.");
        }
    }

    // Method to make a call and send SMS in order
    private void makeEmergencyCallThenSms() {
        SharedPreferences sharedPreferences = getSharedPreferences("PhoneNo", MODE_PRIVATE);
        String callNumber = sharedPreferences.getString("CALLNUM", null);
        String smsNumber1 = sharedPreferences.getString("SMSNUM1", null);
        String smsNumber2 = sharedPreferences.getString("SMSNUM2", null);

        // Make a phone call first
        if (callNumber != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Call permission not granted", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + callNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(callIntent);

            // Delay for the duration of the call, then send SMS
            new Handler().postDelayed(() -> sendSmsWithLocation(smsNumber1, smsNumber2), 10000); // Delay for the call (10 seconds)
        } else {
            Log.e("ShakeDetection", "Call number is null");
        }
    }

    // Method to send SMS with location after the call
    private void sendSmsWithLocation(String smsNumber1, String smsNumber2) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            String googleMapsLink = "Location not available";
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                googleMapsLink = "https://maps.google.com/?q=" + latitude + "," + longitude;
            }
            String message = "Emergency! Please help. Here's my location: " + googleMapsLink;

            if (smsNumber1 != null) {
                sendSms(smsNumber1, message);
            } else {
                Log.e("ShakeDetection", "SMS Number 1 is null");
            }

            if (smsNumber2 != null) {
                sendSms(smsNumber2, message);
            } else {
                Log.e("ShakeDetection", "SMS Number 2 is null");
            }

            Log.d("ShakeDetection", "SMS with location sent: " + googleMapsLink);
        });
    }

    // Method to send SMS
    private void sendSms(String phoneNumber, String message) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Toast.makeText(this, "SMS sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        Log.d("ShakeDetection", "SMS sent to " + phoneNumber);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
