package com.example.studentapp;

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
        setContentView(R.layout.activity_login);

        EditText etEnrollment = findViewById(R.id.et_enrollment);
        EditText etPassword = findViewById(R.id.et_password);
        Button loginButton = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_circular); // Ensure this exists in XML

        loginButton.setOnClickListener(v -> {
            String enrollment = etEnrollment.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(enrollment)) {
                etEnrollment.setError("Student ID is required");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Password is required");
                return;
            }

            performLogin(enrollment, password);
        });

        findViewById(R.id.tv_forgot_password).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        findViewById(R.id.tv_contact_admin).setOnClickListener(v -> {
            String phoneNumber = "+917823079210";
            String message = "Hello Admin, I need help with my Student Bus Tracking account. Please assist me.";
            String url = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + android.net.Uri.encode(message);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(android.net.Uri.parse(url));
            startActivity(i);
        });

        findViewById(R.id.iv_back).setOnClickListener(v -> onBackPressed());
    }

    private void performLogin(String enrollmentId, String password) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("students").child(enrollmentId);
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                
                if (snapshot.exists()) {
                    String dbPass = snapshot.child("password").getValue(String.class);
                    if (password.equals(dbPass)) {
                        saveStudentData(snapshot);
                        // Go to Main
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid Password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Student ID not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveStudentData(DataSnapshot snapshot) {
        SharedPreferences pref = getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("enrollmentId", snapshot.child("enrollmentId").getValue(String.class));
        editor.putString("name", snapshot.child("name").getValue(String.class));
        editor.putString("busNo", snapshot.child("busNo").getValue(String.class));
        editor.putString("feeStatus", snapshot.child("feeStatus").getValue(String.class));
        editor.putString("passStatus", snapshot.child("passStatus").getValue(String.class));
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }
}
