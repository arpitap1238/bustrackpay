package com.example.studentapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity {

    private com.google.android.material.card.MaterialCardView cardSem1, cardSem2, cardFull;
    private String selectedSem = "Sem 1";
    private String selectedAmount = "7500";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        findViewById(R.id.btn_back_payment).setOnClickListener(v -> finish());

        cardSem1 = findViewById(R.id.card_sem1);
        cardSem2 = findViewById(R.id.card_sem2);
        cardFull = findViewById(R.id.card_full_year);

        cardSem1.setOnClickListener(v -> selectSem("Sem 1", "7500"));
        cardSem2.setOnClickListener(v -> selectSem("Sem 2", "7500"));
        cardFull.setOnClickListener(v -> selectSem("Full Year", "15000"));

        TextView tvUpi = findViewById(R.id.tv_upi_id);
        if (tvUpi != null) {
            tvUpi.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("UPI ID", tvUpi.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "UPI ID Copied!", Toast.LENGTH_SHORT).show();
            });
        }

        EditText etUtr = findViewById(R.id.et_utr_number);
        findViewById(R.id.btn_submit_payment).setOnClickListener(v -> {
            String utr = etUtr.getText().toString().trim();
            if (utr.isEmpty()) {
                Toast.makeText(this, "Please enter UTR Number", Toast.LENGTH_SHORT).show();
            } else if (utr.length() < 12) {
                Toast.makeText(this, "Please enter 12-digit UTR Number", Toast.LENGTH_SHORT).show();
            } else {
                processPayment(utr);
            }
        });
    }

    private void selectSem(String sem, String amount) {
        selectedSem = sem;
        selectedAmount = amount;
        
        // Update UI Visuals
        cardSem1.setStrokeWidth(sem.equals("Sem 1") ? 6 : 0);
        cardSem1.setCardBackgroundColor(sem.equals("Sem 1") ? 0xFFEFF6FF : 0xFFFFFFFF);
        
        cardSem2.setStrokeWidth(sem.equals("Sem 2") ? 6 : 0);
        cardSem2.setCardBackgroundColor(sem.equals("Sem 2") ? 0xFFEFF6FF : 0xFFFFFFFF);
        
        cardFull.setStrokeWidth(sem.equals("Full Year") ? 6 : 0);
        cardFull.setCardBackgroundColor(sem.equals("Full Year") ? 0xFFEFF6FF : 0xFFFFFFFF);
    }

    private void processPayment(String utr) {
        android.content.SharedPreferences pref = getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE);
        String enrollmentId = pref.getString("enrollmentId", "Unknown");
        String name = pref.getString("name", "Student");
        String busNo = getIntent().getStringExtra("bus_no");

        if (busNo == null || busNo.isEmpty()) {
            busNo = pref.getString("busNo", "");
        }

        if (busNo == null || busNo.isEmpty() || busNo.equals("Not Assigned") || busNo.equals("..")) {
            Toast.makeText(this, "Error: No Bus Selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Push to Payment Requests for Admin Approval
        com.google.firebase.database.DatabaseReference requestsRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("paymentRequests");
        String requestId = requestsRef.push().getKey();
        
        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("requestId", requestId);
        request.put("enrollmentId", enrollmentId);
        request.put("studentName", name);
        request.put("busNo", busNo);
        request.put("utr", utr);
        request.put("amount", selectedAmount);
        request.put("semester", selectedSem);
        request.put("status", "Pending");
        request.put("timestamp", com.google.firebase.database.ServerValue.TIMESTAMP);

        if (requestId != null) {
            requestsRef.child(requestId).setValue(request).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Payment Request Submitted for Approval", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, PaymentStatusActivity.class);
                intent.putExtra("status", "Pending");
                startActivity(intent);
                finish();
            });
        }
    }
}
