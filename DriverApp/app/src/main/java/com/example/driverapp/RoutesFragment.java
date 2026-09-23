package com.example.driverapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import android.widget.Button;

public class RoutesFragment extends Fragment implements OnMapReadyCallback {
    
    private GoogleMap mMap;
    private Polyline routePolyline;
    private List<Marker> stopMarkers = new ArrayList<>();
    
    private String driverBusNo = "";
    private String driverId = "";
    private DatabaseReference routesRef;
    private DatabaseReference locationRef;
    private DatabaseReference driverRef;
    private ValueEventListener routeListener;
    private ValueEventListener locationListener;
    private ValueEventListener driverListener;
    
    private Marker busMarker;
    private BottomSheetBehavior<View> sheetBehavior;
    private Button btnDutyToggle;
    private TextView tvDutyStatus;
    private View statusDot;
    private boolean isDutyLive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routes, container, false);
        
        SharedPreferences pref = getActivity().getSharedPreferences("DriverPrefs", Context.MODE_PRIVATE);
        driverId = pref.getString("driverId", "");
        driverBusNo = pref.getString("busNo", "");
        
        // Bind UI
        btnDutyToggle = view.findViewById(R.id.btn_duty_toggle);
        tvDutyStatus = view.findViewById(R.id.tv_duty_status);
        statusDot = view.findViewById(R.id.view_status_dot);
        View bottomSheet = view.findViewById(R.id.duty_bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(bottomSheet);

        isDutyLive = isServiceRunning(LocationTrackingService.class);
        updateDutyUI();

        btnDutyToggle.setOnClickListener(v -> toggleDuty());

        setupDriverSync();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.driver_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        routesRef = FirebaseDatabase.getInstance().getReference("routes");
        
        return view;
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void toggleDuty() {
        isDutyLive = !isDutyLive;
        android.content.Intent serviceIntent = new android.content.Intent(getContext(), LocationTrackingService.class);
        if (isDutyLive) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getContext().startForegroundService(serviceIntent);
            } else {
                getContext().startService(serviceIntent);
            }
        } else {
            getContext().stopService(serviceIntent);
        }
        updateDutyUI();
    }

    private void updateDutyUI() {
        if (isDutyLive) {
            btnDutyToggle.setText("STOP DUTY");
            btnDutyToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
            tvDutyStatus.setText("LIVE");
            tvDutyStatus.setTextColor(Color.parseColor("#10B981"));
            statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
            startPulseAnimation(statusDot);
        } else {
            btnDutyToggle.setText("START DUTY");
            btnDutyToggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0F67FD")));
            tvDutyStatus.setText("OFFLINE");
            tvDutyStatus.setTextColor(Color.parseColor("#64748B"));
            statusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#94A3B8")));
            statusDot.clearAnimation();
        }
    }

    private void startPulseAnimation(View v) {
        android.view.animation.Animation pulse = new android.view.animation.AlphaAnimation(1.0f, 0.3f);
        pulse.setDuration(800);
        pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
        pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
        v.startAnimation(pulse);
    }

    private void setupDriverSync() {
        if (driverId.isEmpty()) return;
        driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId);
        driverListener = driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String newBusNo = snapshot.child("busNo").getValue(String.class);
                    if (newBusNo != null && !newBusNo.equals(driverBusNo)) {
                        driverBusNo = newBusNo;
                        // Refresh Route if map is ready
                        if (mMap != null) {
                            fetchRouteAndDraw();
                            startLiveTracking();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        
        // Default location (Vathar)
        LatLng defaultPos = new LatLng(16.8450, 74.2987);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 15));
        
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        fetchRouteAndDraw();
        startLiveTracking();
    }

    private void startLiveTracking() {
        if (driverBusNo.isEmpty()) return;
        
        locationRef = FirebaseDatabase.getInstance().getReference("locations").child(driverBusNo);
        locationListener = locationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mMap == null) return;
                
                Double lat = snapshot.child("latitude").getValue(Double.class);
                Double lng = snapshot.child("longitude").getValue(Double.class);
                
                if (lat != null && lng != null) {
                    LatLng busPos = new LatLng(lat, lng);
                    
                    if (busMarker == null) {
                        busMarker = mMap.addMarker(new MarkerOptions()
                                .position(busPos)
                                .title("My Bus")
                                .icon(getBitmapFromVector(getContext(), R.drawable.ic_bus_logo))
                                .anchor(0.5f, 0.5f)
                                .flat(true));
                        
                        // First time, zoom and center
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(busPos, 16));
                    } else {
                        busMarker.setPosition(busPos);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private com.google.android.gms.maps.model.BitmapDescriptor getBitmapFromVector(Context context, int vectorResId) {
        android.graphics.drawable.Drawable vectorDrawable = androidx.core.content.ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void fetchRouteAndDraw() {
        routeListener = routesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot routeSnap : snapshot.getChildren()) {
                    String bNo = routeSnap.child("busNo").getValue(String.class);
                    if (driverBusNo.equals(bNo)) {
                        updateRouteInfo(routeSnap);
                        drawRouteOnMap(routeSnap);
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void drawRouteOnMap(DataSnapshot routeSnap) {
        if (mMap == null) return;

        if (routePolyline != null) routePolyline.remove();
        for (Marker m : stopMarkers) m.remove();
        stopMarkers.clear();

        PolylineOptions polyOptions = new PolylineOptions()
                .color(Color.parseColor("#2563EB")) // Professional Blue
                .width(14)
                .jointType(com.google.android.gms.maps.model.JointType.ROUND)
                .endCap(new com.google.android.gms.maps.model.RoundCap())
                .startCap(new com.google.android.gms.maps.model.RoundCap())
                .geodesic(true);

        List<LatLng> points = new ArrayList<>();

        // 1. Source
        DataSnapshot source = routeSnap.child("source");
        Double sLat = source.child("lat").getValue(Double.class);
        Double sLng = source.child("lng").getValue(Double.class);
        if (sLat != null && sLng != null) {
            LatLng start = new LatLng(sLat, sLng);
            points.add(start);
            stopMarkers.add(mMap.addMarker(new MarkerOptions().position(start).title("Origin: " + source.child("name").getValue(String.class))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))));
        }

        // 2. Waypoints
        DataSnapshot stops = routeSnap.child("stops");
        for (DataSnapshot stopSnap : stops.getChildren()) {
            Double pLat = stopSnap.child("lat").getValue(Double.class);
            Double pLng = stopSnap.child("lng").getValue(Double.class);
            if (pLat != null && pLng != null) {
                LatLng pos = new LatLng(pLat, pLng);
                points.add(pos);
                stopMarkers.add(mMap.addMarker(new MarkerOptions().position(pos).title(stopSnap.child("name").getValue(String.class))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));
            }
        }

        // 3. Destination
        DataSnapshot dest = routeSnap.child("destination");
        Double dLat = dest.child("lat").getValue(Double.class);
        Double dLng = dest.child("lng").getValue(Double.class);
        if (dLat != null && dLng != null) {
            LatLng end = new LatLng(dLat, dLng);
            points.add(end);
            stopMarkers.add(mMap.addMarker(new MarkerOptions().position(end).title("Terminal: " + dest.child("name").getValue(String.class))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))));
        }

        // 4. Detailed Road-Following Polyline
        String encoded = routeSnap.child("encodedPolyline").getValue(String.class);
        if (encoded != null && !encoded.isEmpty()) {
            List<LatLng> decodedPath = PolylineDecoder.decode(encoded);
            polyOptions.addAll(decodedPath);
            routePolyline = mMap.addPolyline(polyOptions);

            // Zoom to fit entire route
            com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
            for (LatLng p : decodedPath) builder.include(p);
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            } catch (Exception e) {
                if (!decodedPath.isEmpty()) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(decodedPath.get(0), 15));
            }
        } else if (!points.isEmpty()) {
            // Fallback for legacy straight lines
            android.widget.Toast.makeText(getContext(), "Note: Road-following data missing. Resave in Admin Panel.", android.widget.Toast.LENGTH_LONG).show();
            polyOptions.addAll(points);
            routePolyline = mMap.addPolyline(polyOptions);

            com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
            for (LatLng p : points) builder.include(p);
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            } catch (Exception e) {}
        }
    }

    private void updateRouteInfo(DataSnapshot routeSnap) {
        if (getView() == null) return;
        
        TextView tvRoute = getView().findViewById(R.id.tv_route_name);
        TextView tvBus = getView().findViewById(R.id.tv_bus_no);
        ViewGroup layoutStops = getView().findViewById(R.id.layout_stops);
        
        if (tvRoute != null) tvRoute.setText(routeSnap.child("name").getValue(String.class));
        if (tvBus != null) tvBus.setText("Bus #" + driverBusNo);
        
        if (layoutStops != null) {
            layoutStops.removeAllViews();
            
            // 1. Source
            addStopToLayout(layoutStops, routeSnap.child("source").child("name").getValue(String.class), "Source", "#2563EB", true);
            
            // 2. Waypoints
            DataSnapshot stops = routeSnap.child("stops");
            for (DataSnapshot stopSnap : stops.getChildren()) {
                addStopToLayout(layoutStops, stopSnap.child("name").getValue(String.class), "Stop", "#3B82F6", true);
            }
            
            // 3. Destination
            addStopToLayout(layoutStops, routeSnap.child("destination").child("name").getValue(String.class), "Terminal", "#6366F1", false);
        }
    }

    private void addStopToLayout(ViewGroup parent, String name, String type, String colorStr, boolean showLine) {
        Context ctx = getContext();
        if (ctx == null) return;
        
        LinearLayout item = new LinearLayout(ctx);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.TOP);
        header.setPadding(0, dpToPx(4), 0, dpToPx(4));
        
        // 1. TIMELINE INDICATOR (Dot + Line)
        LinearLayout timelineStage = new LinearLayout(ctx);
        timelineStage.setOrientation(LinearLayout.VERTICAL);
        timelineStage.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        timelineStage.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), ViewGroup.LayoutParams.WRAP_CONTENT));

        View dot = new View(ctx);
        int dotSize = type.equals("Source") || type.equals("Terminal") ? dpToPx(14) : dpToPx(10);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
        dot.setLayoutParams(dotLp);
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gd.setColor(Color.parseColor(colorStr));
        // Add a subtle white ring for source/terminal
        if (type.equals("Source") || type.equals("Terminal")) {
            gd.setStroke(dpToPx(2), Color.parseColor("#FFFFFF"));
        }
        dot.setBackground(gd);
        timelineStage.addView(dot);
        
        if (showLine) {
            View line = new View(ctx);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(dpToPx(2), dpToPx(48));
            llp.setMargins(0, dpToPx(4), 0, dpToPx(4));
            line.setLayoutParams(llp);
            line.setBackgroundColor(Color.parseColor("#E2E8F0"));
            timelineStage.addView(line);
        }

        header.addView(timelineStage);
        
        // 2. TEXT GROUP
        LinearLayout textGroup = new LinearLayout(ctx);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.setMargins(dpToPx(20), 0, 0, 0);
        textGroup.setLayoutParams(lp);
        
        TextView tvName = new TextView(ctx);
        tvName.setText(name);
        tvName.setTextSize(17);
        tvName.setTextColor(Color.parseColor("#0F172A"));
        tvName.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        textGroup.addView(tvName);
        
        TextView tvType = new TextView(ctx);
        tvType.setText(type.toUpperCase());
        tvType.setTextSize(11);
        tvType.setTextColor(Color.parseColor("#94A3B8"));
        tvType.setLetterSpacing(0.05f);
        tvType.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        textGroup.addView(tvType);
        
        header.addView(textGroup);

        // 3. ACTION ICON (Optional, e.g., a checkmark for completed)
        if (!showLine && type.equals("Terminal")) {
             ImageView iv = new ImageView(ctx);
             iv.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20)));
             iv.setImageResource(R.drawable.ic_map_pin_white);
             iv.setColorFilter(Color.parseColor("#CBD5E1"));
             header.addView(iv);
        }
        
        item.addView(header);
        parent.addView(item);
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (routesRef != null && routeListener != null) {
            routesRef.removeEventListener(routeListener);
        }
        if (locationRef != null && locationListener != null) {
            locationRef.removeEventListener(locationListener);
        }
        if (driverRef != null && driverListener != null) {
            driverRef.removeEventListener(driverListener);
        }
    }
}
