package com.example.studentapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studentapp.utils.EmailHelper;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Random;

public class ForgotPasswordActivity extends AppCompatActivity {

    private String generatedOtp;
    private String studentId;
    private String studentEmail;
    
    private LinearLayout layoutStep1, layoutStep2, layoutStep3;
    private EditText etEnrollment, etOtp, etNewPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        layoutStep1 = findViewById(R.id.layout_step1);
        layoutStep2 = findViewById(R.id.layout_step2);
        layoutStep3 = findViewById(R.id.layout_step3);

        etEnrollment = findViewById(R.id.et_enrollment);
        etOtp = findViewById(R.id.et_otp);
        etNewPassword = findViewById(R.id.et_new_password);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> onBackPressed());
        }

        findViewById(R.id.btn_send_otp).setOnClickListener(v -> sendOtp());
        findViewById(R.id.btn_verify_otp).setOnClickListener(v -> verifyOtp());
        findViewById(R.id.btn_update_password).setOnClickListener(v -> updatePassword());
    }

    private void sendOtp() {
        studentId = etEnrollment.getText().toString().trim();
        if (TextUtils.isEmpty(studentId)) {
            etEnrollment.setError("Student ID is required");
            return;
        }

        // Fetch student email from Firebase
        FirebaseDatabase.getInstance().getReference("students").child(studentId)
            .get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    studentEmail = snapshot.child("email").getValue(String.class);
                    if (TextUtils.isEmpty(studentEmail)) {
                        Toast.makeText(this, "No email registered for this ID. Contact Admin.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
                    
                    Toast.makeText(this, "Sending OTP to " + studentEmail + "...", Toast.LENGTH_SHORT).show();
                    
                    EmailHelper.sendOtpEmail(studentEmail, generatedOtp, new EmailHelper.EmailCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(ForgotPasswordActivity.this, "OTP Sent Successfully", Toast.LENGTH_SHORT).show();
                                layoutStep1.setVisibility(View.GONE);
                                layoutStep2.setVisibility(View.VISIBLE);
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(() -> Toast.makeText(ForgotPasswordActivity.this, "Failed to send email: " + error, Toast.LENGTH_LONG).show());
                        }
                    });
                } else {
                    Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show());
    }

    private void verifyOtp() {
        String enteredOtp = etOtp.getText().toString().trim();
        if (enteredOtp.equals(generatedOtp)) {
            layoutStep2.setVisibility(View.GONE);
            layoutStep3.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePassword() {
        String newPass = etNewPassword.getText().toString().trim();
        if (TextUtils.isEmpty(newPass)) {
            etNewPassword.setError("Password required");
            return;
        }

        FirebaseDatabase.getInstance().getReference("students").child(studentId)
            .child("password").setValue(newPass)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Password Updated Successfully!", Toast.LENGTH_LONG).show();
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show());
    }
}
