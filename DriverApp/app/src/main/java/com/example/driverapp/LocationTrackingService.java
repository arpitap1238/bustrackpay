package com.example.driverapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class LocationTrackingService extends Service {

    private static final String TAG = "LocationTrackingService";
    private static final String CHANNEL_ID = "DriverLocationChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference locationRef;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        android.content.SharedPreferences pref = getSharedPreferences("DriverPrefs", android.content.Context.MODE_PRIVATE);
        String driverId = pref.getString("driverId", "unknown_driver");
        String driverName = pref.getString("name", "Driver");
        String busNo = pref.getString("busNo", "00");

        locationRef = FirebaseDatabase.getInstance().getReference("locations").child(busNo);
        
        // Initial setup for the node
        Map<String, Object> meta = new HashMap<>();
        meta.put("name", driverName);
        meta.put("busNo", busNo);
        meta.put("phone", pref.getString("phone", ""));
        meta.put("status", "Active");
        locationRef.updateChildren(meta);

        createNotificationChannel();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();
                    Log.d(TAG, "Location Update: " + lat + ", " + lng);

                    Map<String, Object> locData = new HashMap<>();
                    locData.put("latitude", lat);
                    locData.put("longitude", lng);
                    locData.put("updatedAt", System.currentTimeMillis());

                    locationRef.updateChildren(locData);
                }
            }
        };
    }

    public static final String ACTION_STOP_TRACKING = "com.example.driverapp.STOP_TRACKING";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_TRACKING.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // 1. Intent to open the app on tap
        Intent mainIntent = new Intent(this, MainActivity.class);
        android.app.PendingIntent mainPendingIntent = android.app.PendingIntent.getActivity(
                this, 0, mainIntent, android.app.PendingIntent.FLAG_IMMUTABLE);

        // 2. Intent to stop duty from notification
        Intent stopIntent = new Intent(this, LocationTrackingService.class);
        stopIntent.setAction(ACTION_STOP_TRACKING);
        android.app.PendingIntent stopPendingIntent = android.app.PendingIntent.getService(
                this, 0, stopIntent, android.app.PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Duty Live: Broadcasting Location")
                .setContentText("Students can see your bus on the map.")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(mainPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP DUTY", stopPendingIntent)
                .setColor(android.graphics.Color.parseColor("#0F67FD"))
                .build();

        startForeground(1, notification);
        startLocationUpdates();

        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(3000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Live Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (locationRef != null) {
            locationRef.child("status").setValue("Offline");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
