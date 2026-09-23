package com.example.driverapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardFragment extends Fragment {

    private boolean isTracking = false;
    private View layoutHud;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private Button btnToggleTracking;

    private DatabaseReference driverRef;
    private ValueEventListener driverListener;
    private String driverId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        btnToggleTracking = view.findViewById(R.id.btn_toggle_tracking);
        layoutHud = view.findViewById(R.id.layout_tracking_hud);
        tvTitle = view.findViewById(R.id.tv_tracking_title);
        tvSubtitle = view.findViewById(R.id.tv_tracking_subtitle);

        if (getActivity() != null) {
            android.content.SharedPreferences pref = getActivity().getSharedPreferences("DriverPrefs", android.content.Context.MODE_PRIVATE);
            driverId = pref.getString("driverId", "");
            
            isTracking = isServiceRunning(LocationTrackingService.class);
            setupRealtimeSync();
        }

        setupHeaderUI();
        updateTrackingUI();

        btnToggleTracking.setOnClickListener(v -> {
            isTracking = !isTracking;
            updateTrackingUI();
        });

        // Service Card Clicks
        view.findViewById(R.id.card_routes).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateNavState(new RoutesFragment(), getActivity().findViewById(R.id.custom_nav_routes), "Routes");
            }
        });

        view.findViewById(R.id.card_students).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateNavState(new StudentsFragment(), getActivity().findViewById(R.id.custom_nav_students), "Students");
            }
        });

        view.findViewById(R.id.card_notifications).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateNavState(new NotificationsFragment(), null, "Notifications");
            }
        });

        view.findViewById(R.id.card_support).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateNavState(new SupportFragment(), null, "Help & Support");
            }
        });

        return view;
    }

    private void setupRealtimeSync() {
        if (driverId.isEmpty()) return;
        
        driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId);
        driverListener = driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && getActivity() != null) {
                    String name = snapshot.child("name").getValue(String.class);
                    String busNo = snapshot.child("busNo").getValue(String.class);
                    
                    // Update SharedPreferences
                    android.content.SharedPreferences pref = getActivity().getSharedPreferences("DriverPrefs", android.content.Context.MODE_PRIVATE);
                    android.content.SharedPreferences.Editor editor = pref.edit();
                    if (name != null) editor.putString("name", name);
                    if (busNo != null) editor.putString("busNo", busNo);
                    editor.apply();
                    
                    // Refresh UI
                    setupHeaderUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupHeaderUI() {
        if (getActivity() != null) {
            android.content.SharedPreferences pref = getActivity().getSharedPreferences("DriverPrefs", android.content.Context.MODE_PRIVATE);
            String name = pref.getString("name", "Driver");
            String busNo = pref.getString("busNo", "..");
            
            if (!isTracking) {
                tvTitle.setText(name);
                tvSubtitle.setText("Bus #" + busNo + " Assigned");
            }
        }
    }

    private void updateTrackingUI() {
        if (isTracking) {
            layoutHud.setBackgroundResource(R.drawable.bg_featured_card_active);
            tvTitle.setText("Tracking Live");
            tvSubtitle.setText("Broadcasting position securely");
            btnToggleTracking.setText("PAUSE TRACKING");
            btnToggleTracking.setTextColor(Color.parseColor("#EF4444"));

            if (getView() != null) {
                View pulse = getView().findViewById(R.id.view_pulse);
                TextView status = getView().findViewById(R.id.tv_status_pill);
                if (pulse != null) {
                    pulse.setVisibility(View.VISIBLE);
                    startPulseAnimation(pulse);
                }
                if (status != null) status.setText("LIVE");
            }
            
            Intent serviceIntent = new Intent(getContext(), LocationTrackingService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getContext().startForegroundService(serviceIntent);
            } else {
                getContext().startService(serviceIntent);
            }

            // Redirect to Map
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateNavState(new RoutesFragment(), getActivity().findViewById(R.id.custom_nav_routes), "Routes");
            }
        } else {
            layoutHud.setBackgroundResource(R.drawable.bg_featured_card);
            tvTitle.setText("Tracking Offline");
            tvSubtitle.setText("Tap GO LIVE to start broadcast");
            btnToggleTracking.setText("GO LIVE");
            btnToggleTracking.setTextColor(Color.parseColor("#2563EB"));

            if (getView() != null) {
                View pulse = getView().findViewById(R.id.view_pulse);
                TextView status = getView().findViewById(R.id.tv_status_pill);
                if (pulse != null) {
                    pulse.clearAnimation();
                    pulse.setVisibility(View.GONE);
                }
                if (status != null) status.setText("OFFLINE");
            }
            
            setupHeaderUI();
            
            getContext().stopService(new Intent(getContext(), LocationTrackingService.class));
        }
    }

    private void startPulseAnimation(View pulseView) {
        if (pulseView == null) return;
        android.view.animation.Animation pulse = new android.view.animation.AlphaAnimation(1.0f, 0.2f);
        pulse.setDuration(800);
        pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
        pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
        pulseView.startAnimation(pulse);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        if (getActivity() == null) return false;
        android.app.ActivityManager manager = (android.app.ActivityManager) getActivity().getSystemService(android.content.Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (driverRef != null && driverListener != null) {
            driverRef.removeEventListener(driverListener);
        }
    }
}
