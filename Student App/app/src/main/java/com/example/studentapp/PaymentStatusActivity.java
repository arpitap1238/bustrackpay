package com.example.studentapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;

public class PaymentStatusActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_status);

        // GPay-style Pop Animation
        ImageView ivTick = findViewById(R.id.iv_success_tick);
        ivTick.setScaleX(0f);
        ivTick.setScaleY(0f);
        ivTick.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(100)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                .start();

        // Share Receipt Logic
        findViewById(R.id.btn_share_receipt).setOnClickListener(v -> {
            shareReceipt();
        });

        // Download Simulation Logic
        findViewById(R.id.btn_download_pdf).setOnClickListener(v -> {
            Toast.makeText(this, "Receipt Downloaded to Downloads/Receipts", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_status_done).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void shareReceipt() {
        String txDetails = "COLLEGE TRANSPORT BILLING\n" +
                "--------------------------\n" +
                "Transaction: SUCCESSFUL\n" +
                "Amount: ₹4,000.00\n" +
                "Reference: VLT-90821-X\n" +
                "Status: Authorized by Student\n" +
                "Message: Admin verification in progress.\n\n" +
                "Shared via College Bus Tracking App";

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, txDetails);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Share Receipt Via");
        startActivity(shareIntent);
    }
}
