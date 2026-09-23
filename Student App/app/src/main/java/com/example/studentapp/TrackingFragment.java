package com.example.studentapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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

public class TrackingFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker busMarker;
    private Polyline coveredPolyline;
    private Polyline remainingPolyline;
    private List<LatLng> fullPathPoints = new ArrayList<>();
    private List<Marker> stopMarkers = new ArrayList<>();
    
    private String studentBusNo = "";
    private String studentPickupStop = "";
    private DatabaseReference locationsRef;
    private DatabaseReference routesRef;
    private ValueEventListener syncListener;
    private ValueEventListener routeListener;
    
    private DatabaseReference studentRef;
    private ValueEventListener studentListener;

    private String currentRouteName = "";
    private String destinationName = "";
    private int totalStops = 0;
    private List<DataSnapshot> routeStopsData = new ArrayList<>();

    private android.os.Handler handler = new android.os.Handler();
    private boolean isBusLive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracking, container, false);

        SharedPreferences pref = getActivity().getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE);
        studentBusNo = pref.getString("busNo", "");

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        locationsRef = FirebaseDatabase.getInstance().getReference("locations");
        routesRef = FirebaseDatabase.getInstance().getReference("routes");

        // Set initial placeholder texts
        TextView tvInfo = view.findViewById(R.id.tv_tracking_route_info);
        TextView tvNext = view.findViewById(R.id.tv_tracking_next_stop);
        if (tvInfo != null) tvInfo.setText("Bus: " + (studentBusNo.isEmpty() ? "Unassigned" : "#" + studentBusNo));
        if (tvNext != null) tvNext.setText("Routing: Syncing with Satellite...");

        startPulseAnimation(view.findViewById(R.id.view_pulse));

        return view;
    }

    private void startPulseAnimation(View pulseView) {
        if (pulseView == null) return;
        android.view.animation.Animation pulse = new android.view.animation.AlphaAnimation(1.0f, 0.2f);
        pulse.setDuration(800);
        pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
        pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
        pulseView.startAnimation(pulse);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        
        // Initial Center (Vathar area)
        LatLng defaultPos = new LatLng(16.8450, 74.2987); 
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 15));

        // 1. First, fetch the latest student assignment to ensure absolute data accuracy
        String enrollmentId = getContext().getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE).getString("enrollmentId", "");
        if (!enrollmentId.isEmpty()) {
            studentRef = FirebaseDatabase.getInstance().getReference("students").child(enrollmentId);
            studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String latestBus = snapshot.child("busNo").getValue(String.class);
                        if (latestBus != null) {
                            studentBusNo = latestBus;
                            studentPickupStop = snapshot.child("pickupStop").getValue(String.class);
                            android.util.Log.d("TrackingFragment", "Verified Bus: " + studentBusNo + ", Pickup: " + studentPickupStop);
                            
                            // Initialize UI with confirmed data
                            if (getView() != null) {
                                TextView tvInfo = getView().findViewById(R.id.tv_tracking_route_info);
                                String tripLine = (studentPickupStop != null && !studentPickupStop.isEmpty()) ? 
                                    "From " + studentPickupStop + " to College" : "Loading Route...";
                                if (tvInfo != null) tvInfo.setText(tripLine);
                            }
                            
                            fetchRouteAndDraw();
                            startTrackingSync();
                        } else {
                            handleNoAssignment();
                        }
                    } else {
                        handleNoAssignment();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    handleNoAssignment();
                }
            });
        } else {
            handleNoAssignment();
        }
    }

    private void handleNoAssignment() {
        if (getView() != null) {
            TextView tvInfo = getView().findViewById(R.id.tv_tracking_route_info);
            TextView tvNext = getView().findViewById(R.id.tv_tracking_next_stop);
            if (tvInfo != null) tvInfo.setText("No Bus Assigned");
            if (tvNext != null) tvNext.setText("Contact Administrator");
        }
    }

    private void fetchRouteAndDraw() {
        routeListener = routesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean routeFound = false;
                for (DataSnapshot routeSnap : snapshot.getChildren()) {
                    String bNo = String.valueOf(routeSnap.child("busNo").getValue()); // Handle as String
                    if (studentBusNo.equals(bNo)) {
                        android.util.Log.d("TrackingFragment", "Route found for bus: " + bNo);
                        drawRouteOnMap(routeSnap);
                        routeFound = true;
                        break;
                    }
                }
                if (!routeFound) {
                    android.util.Log.w("TrackingFragment", "No route found for bus: " + studentBusNo);
                    if (getView() != null) {
                        TextView tvInfo = getView().findViewById(R.id.tv_tracking_route_info);
                        if (tvInfo != null) tvInfo.setText("Status: Route Not Provisioned");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void drawRouteOnMap(DataSnapshot routeSnap) {
        if (mMap == null) return;

        // Clear existing route
        if (coveredPolyline != null) coveredPolyline.remove();
        if (remainingPolyline != null) remainingPolyline.remove();
        coveredPolyline = null;
        remainingPolyline = null;
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
            String name = source.child("name").getValue(String.class);
            float color = (name != null && name.equals(studentPickupStop)) ? BitmapDescriptorFactory.HUE_ORANGE : BitmapDescriptorFactory.HUE_GREEN;
            stopMarkers.add(mMap.addMarker(new MarkerOptions().position(start).title(name)
                    .icon(BitmapDescriptorFactory.defaultMarker(color))));
        }

        // 2. Waypoints
        routeStopsData.clear();
        DataSnapshot stops = routeSnap.child("stops");
        for (DataSnapshot stopSnap : stops.getChildren()) {
            routeStopsData.add(stopSnap);
            Double pLat = stopSnap.child("lat").getValue(Double.class);
            Double pLng = stopSnap.child("lng").getValue(Double.class);
            if (pLat != null && pLng != null) {
                LatLng pos = new LatLng(pLat, pLng);
                points.add(pos);
                String name = stopSnap.child("name").getValue(String.class);
                float color = (name != null && name.equals(studentPickupStop)) ? BitmapDescriptorFactory.HUE_ORANGE : BitmapDescriptorFactory.HUE_AZURE;
                stopMarkers.add(mMap.addMarker(new MarkerOptions().position(pos).title(name)
                        .icon(BitmapDescriptorFactory.defaultMarker(color))));
            }
        }

        // 3. Destination
        DataSnapshot dest = routeSnap.child("destination");
        Double dLat = dest.child("lat").getValue(Double.class);
        Double dLng = dest.child("lng").getValue(Double.class);
        if (dLat != null && dLng != null) {
            LatLng end = new LatLng(dLat, dLng);
            points.add(end);
            stopMarkers.add(mMap.addMarker(new MarkerOptions().position(end).title("End: " + dest.child("name").getValue(String.class))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))));
        }

        // 4. Detailed Road-Following Polyline
        String encoded = routeSnap.child("encodedPolyline").getValue(String.class);
        if (encoded != null && !encoded.isEmpty()) {
            fullPathPoints = PolylineDecoder.decode(encoded);
            drawDualPolyline(null); // Initial state

            // Zoom to fit entire route
            com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
            for (LatLng p : fullPathPoints) builder.include(p);
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            } catch (Exception e) {
                if (!fullPathPoints.isEmpty()) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(fullPathPoints.get(0), 15));
            }
        } else if (!points.isEmpty()) {
            // Fallback for legacy straight lines
            android.widget.Toast.makeText(getContext(), "Note: Road-following data missing for this route. Resave in Admin Panel.", android.widget.Toast.LENGTH_LONG).show();
            PolylineOptions fallbackOptions = new PolylineOptions()
                    .addAll(points)
                    .width(14)
                    .color(Color.parseColor("#2563EB"))
                    .jointType(com.google.android.gms.maps.model.JointType.ROUND);
            remainingPolyline = mMap.addPolyline(fallbackOptions);
            
            com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
            for (LatLng p : points) builder.include(p);
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            } catch (Exception e) {}
        }

        // Update UI with static route details
        currentRouteName = routeSnap.child("name").getValue(String.class);
        destinationName = routeSnap.child("destination").child("name").getValue(String.class);
        totalStops = (int) routeSnap.child("stops").getChildrenCount();

        if (getView() != null) {
            TextView tvNext = getView().findViewById(R.id.tv_tracking_next_stop);
            if (tvNext != null) tvNext.setText(destinationName);
            
            TextView tvEta = getView().findViewById(R.id.tv_tracking_eta);
            if (tvEta != null) tvEta.setText(totalStops + " Waypoints");

            TextView tvInfo = getView().findViewById(R.id.tv_tracking_route_info);
            if (tvInfo != null) tvInfo.setText(currentRouteName);
            
            updateTimeline(null); // Initial build
        }
    }

    private void startTrackingSync() {
        if (studentBusNo == null || studentBusNo.isEmpty()) return;
        
        syncListener = locationsRef.child(studentBusNo).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                Double lat = snapshot.child("latitude").getValue(Double.class);
                Double lng = snapshot.child("longitude").getValue(Double.class);
                String dName = snapshot.child("name").getValue(String.class);
                String dPhone = snapshot.child("phone").getValue(String.class);

                if (lat != null && lng != null) {
                    LatLng pos = new LatLng(lat, lng);
                    updateMarker(pos, "Bus #" + studentBusNo + " (" + dName + ")");
                    
                    if (getView() != null) {
                        TextView tvInfo = getView().findViewById(R.id.tv_tracking_route_info);
                        TextView tvSpeed = getView().findViewById(R.id.tv_tracking_speed);
                        View pulse = getView().findViewById(R.id.view_pulse);
                        TextView tvPill = getView().findViewById(R.id.tv_status_pill);
                        android.view.View btnCall = getView().findViewById(R.id.btn_call_driver);

                        if (tvInfo != null) tvInfo.setText(currentRouteName);
                        
                        // Handle Call Button
                        if (btnCall != null) {
                            if (dPhone != null && !dPhone.isEmpty()) {
                                btnCall.setVisibility(android.view.View.VISIBLE);
                                btnCall.setOnClickListener(v -> {
                                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
                                    intent.setData(android.net.Uri.parse("tel:" + dPhone));
                                    startActivity(intent);
                                });
                            } else {
                                btnCall.setVisibility(android.view.View.GONE);
                            }
                        }

                        Long lastTime = snapshot.child("updatedAt").getValue(Long.class);
                        String timeStr = "Live";
                        View offlineBanner = getView().findViewById(R.id.layout_offline_banner);
                        TextView tvOfflineMsg = getView().findViewById(R.id.tv_offline_msg);

                        if (lastTime != null) {
                            long diff = System.currentTimeMillis() - lastTime;
                            if (diff > 30000) { // More than 30s
                                int mins = (int) (diff / 60000);
                                timeStr = "Last seen " + (mins > 0 ? mins + "m" : "seconds") + " ago";
                                
                                if (pulse != null) pulse.setVisibility(View.INVISIBLE);
                                if (tvPill != null) {
                                    tvPill.setText("OFFLINE");
                                    tvPill.setTextColor(Color.GRAY);
                                }
                                if (offlineBanner != null) {
                                    offlineBanner.setVisibility(View.VISIBLE);
                                    if (tvOfflineMsg != null) tvOfflineMsg.setText("Offline: " + timeStr);
                                }
                                if (busMarker != null) busMarker.setAlpha(0.6f);
                            } else {
                                if (pulse != null) pulse.setVisibility(View.VISIBLE);
                                if (tvPill != null) {
                                    tvPill.setText("LIVE");
                                    tvPill.setTextColor(Color.parseColor("#10B981"));
                                }
                                if (offlineBanner != null) offlineBanner.setVisibility(View.GONE);
                                if (busMarker != null) busMarker.setAlpha(1.0f);
                            }
                        }
                        
                        if (tvSpeed != null) tvSpeed.setText("Driver: " + dName + " • " + timeStr);
                        drawDualPolyline(pos);
                        updateTimeline(pos);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateMarker(LatLng pos, String title) {
        if (mMap == null) return;
        
        if (busMarker == null) {
            busMarker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(title)
                    .icon(bitmapDescriptorFromVector(R.drawable.ic_bus_logo, 48)) // Scaled up to 48dp
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .zIndex(1.0f));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16));
        } else {
            busMarker.setPosition(pos);
            busMarker.setTitle(title);
        }
    }

    private void drawDualPolyline(LatLng busPos) {
        if (mMap == null || fullPathPoints.isEmpty()) return;

        int splitIndex = 0;
        if (busPos != null) {
            splitIndex = findClosestPointIndex(busPos, fullPathPoints);
        }

        List<LatLng> coveredPoints = fullPathPoints.subList(0, splitIndex + 1);
        List<LatLng> remainingPoints = fullPathPoints.subList(splitIndex, fullPathPoints.size());

        // 1. Covered (Gray)
        if (coveredPolyline == null) {
            coveredPolyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(coveredPoints)
                    .width(14)
                    .color(Color.parseColor("#94A3B8")) // Slate-400
                    .jointType(com.google.android.gms.maps.model.JointType.ROUND)
                    .startCap(new com.google.android.gms.maps.model.RoundCap())
                    .endCap(new com.google.android.gms.maps.model.RoundCap()));
        } else {
            coveredPolyline.setPoints(coveredPoints);
        }

        // 2. Remaining (Blue)
        if (remainingPolyline == null) {
            remainingPolyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(remainingPoints)
                    .width(14)
                    .color(Color.parseColor("#2563EB")) // Blue-600
                    .jointType(com.google.android.gms.maps.model.JointType.ROUND)
                    .startCap(new com.google.android.gms.maps.model.RoundCap())
                    .endCap(new com.google.android.gms.maps.model.RoundCap()));
        } else {
            remainingPolyline.setPoints(remainingPoints);
        }
    }

    private int findClosestPointIndex(LatLng pos, List<LatLng> path) {
        if (pos == null || path == null || path.isEmpty()) return -1;
        int index = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < path.size(); i++) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(pos.latitude, pos.longitude, path.get(i).latitude, path.get(i).longitude, results);
            if (results[0] < minDistance) {
                minDistance = results[0];
                index = i;
            }
        }
        return index;
    }

    private BitmapDescriptor bitmapDescriptorFromVector(int vectorResId, int sizeDp) {
        Drawable vectorDrawable = ContextCompat.getDrawable(getContext(), vectorResId);
        if (vectorDrawable == null) return null;
        
        int sizePx = (int) (sizeDp * getContext().getResources().getDisplayMetrics().density);
        vectorDrawable.setBounds(0, 0, sizePx, sizePx);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void updateTimeline(LatLng busPos) {
        if (getView() == null || routeStopsData.isEmpty() || fullPathPoints.isEmpty()) return;
        ViewGroup layout = getView().findViewById(R.id.layout_timeline);
        TextView tvEta = getView().findViewById(R.id.tv_tracking_eta);
        if (layout == null) return;
        layout.removeAllViews();

        int busPathIndex = findClosestPointIndex(busPos, fullPathPoints);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        
        String nextStopName = "";
        float nextStopDist = -1;

        for (int i = 0; i < routeStopsData.size(); i++) {
            DataSnapshot stop = routeStopsData.get(i);
            String name = stop.child("name").getValue(String.class);
            Double lat = stop.child("lat").getValue(Double.class);
            Double lng = stop.child("lng").getValue(Double.class);

            if (lat != null && lng != null) {
                LatLng stopPos = new LatLng(lat, lng);
                int stopPathIndex = findClosestPointIndex(stopPos, fullPathPoints);

                View item = inflater.inflate(R.layout.item_timeline_stop, layout, false);
                TextView tvName = item.findViewById(R.id.tv_stop_name);
                TextView tvStatus = item.findViewById(R.id.tv_stop_status);
                ImageView indicator = item.findViewById(R.id.view_stop_indicator);
                View line = item.findViewById(R.id.view_timeline_line);
                
                tvName.setText(name);

                if (busPos != null) {
                    float[] dist = new float[1];
                    android.location.Location.distanceBetween(busPos.latitude, busPos.longitude, lat, lng, dist);

                    if (dist[0] < 100) { // Arrived/Closest
                        indicator.setImageResource(R.drawable.ic_stop_active);
                        tvName.setTextColor(Color.parseColor("#10B981"));
                        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvStatus.setText("Arriving Now");
                        tvStatus.setTextColor(Color.parseColor("#10B981"));
                    } else if (busPathIndex != -1 && stopPathIndex != -1 && busPathIndex > stopPathIndex) { // Passed
                        indicator.setImageResource(R.drawable.ic_stop_passed);
                        tvName.setTextColor(Color.LTGRAY);
                        tvName.setPaintFlags(tvName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                        tvStatus.setText("Passed");
                        tvStatus.setTextColor(Color.LTGRAY);
                    } else { // Upcoming
                        indicator.setImageResource(R.drawable.ic_stop_upcoming);
                        tvName.setTextColor(Color.GRAY);
                        tvStatus.setText("Upcoming");
                        
                        if (nextStopDist == -1) {
                            nextStopDist = dist[0];
                            nextStopName = name;
                        }
                    }
                } else {
                    // Initial build / Bus offline
                    indicator.setImageResource(R.drawable.ic_stop_upcoming);
                    tvName.setTextColor(Color.GRAY);
                    tvStatus.setText("Scheduled");
                }

                if (i == routeStopsData.size() - 1) line.setVisibility(View.GONE);
                layout.addView(item);
            }
        }
        
        if (tvEta != null) {
            if (nextStopDist != -1) {
                int mins = (int) (nextStopDist / 416); // 25km/h ≈ 416m/min
                if (mins < 1) tvEta.setText("Arriving at " + nextStopName);
                else tvEta.setText(mins + " min to " + nextStopName);
            } else {
                tvEta.setText("Arrived at Terminal");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationsRef != null && syncListener != null) {
            locationsRef.removeEventListener(syncListener);
        }
        if (routesRef != null && routeListener != null) {
            routesRef.removeEventListener(routeListener);
        }
    }
}
