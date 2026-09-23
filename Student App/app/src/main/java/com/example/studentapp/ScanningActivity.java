package com.example.studentapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

public class ScanningActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);

        View scannerLine = findViewById(R.id.scanner_line);
        View scannerBox = findViewById(R.id.scanner_box);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        // Convert dp to pixels for animation distance
        float density = getResources().getDisplayMetrics().density;
        float distance = 160 * density; // Move ~160dp down (card height is 200dp)

        // Animate the line moving up and down
        ObjectAnimator lineAnim = ObjectAnimator.ofFloat(scannerLine, "translationY", 0f, distance);
        lineAnim.setDuration(1200);
        lineAnim.setRepeatCount(ValueAnimator.INFINITE);
        lineAnim.setRepeatMode(ValueAnimator.REVERSE);
        lineAnim.setInterpolator(new LinearInterpolator());
        lineAnim.start();
        
        // Animate the box moving up and down
        ObjectAnimator boxAnim = ObjectAnimator.ofFloat(scannerBox, "translationY", 0f, distance);
        boxAnim.setDuration(1200);
        boxAnim.setRepeatCount(ValueAnimator.INFINITE);
        boxAnim.setRepeatMode(ValueAnimator.REVERSE);
        boxAnim.setInterpolator(new LinearInterpolator());
        boxAnim.start();

        // Animate the progress bar over 3.5 seconds
        ObjectAnimator progressAnim = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        progressAnim.setDuration(3500);
        progressAnim.setInterpolator(new LinearInterpolator());
        progressAnim.start();

        // Transition to MainActivity after scan completes
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(ScanningActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 3600);
    }
}
