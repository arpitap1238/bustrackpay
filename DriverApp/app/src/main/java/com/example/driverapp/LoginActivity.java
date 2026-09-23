package com.example.driverapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences pref = getSharedPreferences("DriverPrefs", Context.MODE_PRIVATE);
        if (pref.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_login);

        EditText etDriverId = findViewById(R.id.et_driver_id);
        EditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.loading_progress); // Add if missing

        btnLogin.setOnClickListener(v -> {
            String driverId = (etDriverId.getText() != null) ? etDriverId.getText().toString().trim() : "";
            String password = (etPassword.getText() != null) ? etPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(driverId) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please enter both Driver ID and Password", Toast.LENGTH_SHORT).show();
            } else {
                performLogin(driverId, password);
            }
        });
    }

    private void performLogin(String driverId, String password) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("drivers").child(driverId);
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (snapshot.exists()) {
                    String dbPass = snapshot.child("password").getValue(String.class);
                    if (password.equals(dbPass)) {
                        saveDriverData(snapshot);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid Password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Driver ID not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveDriverData(DataSnapshot snapshot) {
        SharedPreferences pref = getSharedPreferences("DriverPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("driverId", snapshot.child("driverId").getValue(String.class));
        editor.putString("name", snapshot.child("name").getValue(String.class));
        editor.putString("busNo", snapshot.child("busNo").getValue(String.class));
        editor.putString("phone", snapshot.child("phone").getValue(String.class));
        editor.putString("licenseNo", snapshot.child("licenseNo").getValue(String.class));
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }
}
