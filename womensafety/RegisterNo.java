package com.example.womensafety;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class RegisterNo extends AppCompatActivity {

    private EditText editTextCallNum, editTextSmsNum1, editTextSmsNum2;
    private Button buttonFinish;

    private static final int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_no);

        // Initialize UI elements
        editTextCallNum = findViewById(R.id.editTextCallNum);
        editTextSmsNum1 = findViewById(R.id.editTextSmsNum1);
        editTextSmsNum2 = findViewById(R.id.editTextSmsNum2);
        buttonFinish = findViewById(R.id.buttonFinish);

        // Load previously saved numbers
        loadRegisteredNumbers();

        // Set up button click listener to save numbers
        buttonFinish.setOnClickListener(v -> {
            if (validateInputs()) {
                if (checkAndRequestPermissions()) {
                    saveRegisteredNumbers();
                    Toast.makeText(RegisterNo.this, "Numbers saved successfully!", Toast.LENGTH_SHORT).show();

                    // Start MainActivity after saving numbers
                    Log.d("RegisterNo", "Numbers saved, starting MainActivity...");
                    Intent intent = new Intent(RegisterNo.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Close RegisterNo activity
                }
            }
        });
    }

    // Method to check and request permissions for SMS and Call
    private boolean checkAndRequestPermissions() {
        String[] permissions = {Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS};

        boolean permissionsGranted = true;

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsGranted = false;
                break;
            }
        }

        if (!permissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE);
        }

        return permissionsGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            boolean permissionsGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted = false;
                    break;
                }
            }

            if (!permissionsGranted) {
                Toast.makeText(this, "Permissions required to save numbers and perform actions.", Toast.LENGTH_SHORT).show();
            } else {
                saveRegisteredNumbers();
                Toast.makeText(this, "Numbers saved successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterNo.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close RegisterNo activity
            }
        }
    }

    // Save the numbers to SharedPreferences
    private void saveRegisteredNumbers() {
        String callNumber = editTextCallNum.getText().toString();
        String smsNumber1 = editTextSmsNum1.getText().toString();
        String smsNumber2 = editTextSmsNum2.getText().toString();

        SharedPreferences sharedPreferences = getSharedPreferences("PhoneNo", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("CALLNUM", callNumber);
        editor.putString("SMSNUM1", smsNumber1);
        editor.putString("SMSNUM2", smsNumber2);
        editor.apply();

        Log.d("RegisterNo", "Numbers saved: Call=" + callNumber + ", SMS1=" + smsNumber1 + ", SMS2=" + smsNumber2);
    }

    // Load the saved numbers and display them in the EditText fields
    private void loadRegisteredNumbers() {
        SharedPreferences sharedPreferences = getSharedPreferences("PhoneNo", MODE_PRIVATE);
        String callNumber = sharedPreferences.getString("CALLNUM", "");
        String smsNumber1 = sharedPreferences.getString("SMSNUM1", "");
        String smsNumber2 = sharedPreferences.getString("SMSNUM2", "");

        editTextCallNum.setText(callNumber);
        editTextSmsNum1.setText(smsNumber1);
        editTextSmsNum2.setText(smsNumber2);

        Log.d("RegisterNo", "Numbers loaded: Call=" + callNumber + ", SMS1=" + smsNumber1 + ", SMS2=" + smsNumber2);
    }

    // Validate the inputs before saving
    private boolean validateInputs() {
        String callNumber = editTextCallNum.getText().toString().trim();
        String smsNumber1 = editTextSmsNum1.getText().toString().trim();
        String smsNumber2 = editTextSmsNum2.getText().toString().trim();

        // Check if any of the fields are empty
        if (callNumber.isEmpty() || smsNumber1.isEmpty() || smsNumber2.isEmpty()) {
            Toast.makeText(this, "Enter the mobile numbers", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check for specific empty fields
        if (callNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a valid call number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (smsNumber1.isEmpty()) {
            Toast.makeText(this, "Please enter a valid SMS number", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (smsNumber2.isEmpty()) {
            Toast.makeText(this, "Please enter a valid police station number", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Basic number length validation (you can adjust the length check as per your needs)
        if (callNumber.length() < 7 || callNumber.length() > 15) {
            Toast.makeText(this, "Call number should be between 7 and 15 digits", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (smsNumber1.length() < 7 || smsNumber1.length() > 15) {
            Toast.makeText(this, "SMS number should be between 7 and 15 digits", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (smsNumber2.length() < 3 || smsNumber2.length() > 15) {
            Toast.makeText(this, "Police Station SMS number should be between 3 and 15 digits", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}
