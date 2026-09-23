package com.example.driverapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private boolean isTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        Button btnToggleTracking = findViewById(R.id.btn_toggle_tracking);
        View layoutHud = findViewById(R.id.layout_tracking_hud);
        TextView tvTitle = findViewById(R.id.tv_tracking_title);
        TextView tvSubtitle = findViewById(R.id.tv_tracking_subtitle);

        btnToggleTracking.setOnClickListener(v -> {
            isTracking = !isTracking;
            if (isTracking) {
                // Tracking Active (Green high-end card)
                layoutHud.setBackgroundResource(R.drawable.bg_featured_card_active);
                tvTitle.setText("Tracking Live");
                tvSubtitle.setText("Broadcasting position securely");
                btnToggleTracking.setText("PAUSE TRACKING");
                btnToggleTracking.setTextColor(Color.parseColor("#10B981"));
                
                Intent serviceIntent = new Intent(DashboardActivity.this, LocationTrackingService.class);
                if (androidx.core.content.ContextCompat.checkSelfPermission(DashboardActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(DashboardActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                }
            } else {
                // Tracking Offline (Blue high-end card)
                layoutHud.setBackgroundResource(R.drawable.bg_featured_card);
                tvTitle.setText("Tracking Offline");
                tvSubtitle.setText("Tap to start transmitting location");
                btnToggleTracking.setText("GO LIVE");
                btnToggleTracking.setTextColor(Color.parseColor("#2563EB"));
                
                stopService(new Intent(DashboardActivity.this, LocationTrackingService.class));
            }
        });

        findViewById(R.id.card_students).setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, StudentListActivity.class));
        });

        findViewById(R.id.card_route_info).setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, RouteDetailsActivity.class));
        });
    }
}
