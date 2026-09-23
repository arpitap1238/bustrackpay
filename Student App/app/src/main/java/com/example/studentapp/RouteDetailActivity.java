package com.example.studentapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RouteDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detail);

        String routeName = getIntent().getStringExtra("route_name");
        String busNo = getIntent().getStringExtra("bus_no");
        String fee = getIntent().getStringExtra("fee");

        ((TextView) findViewById(R.id.tv_detail_route_name)).setText(routeName);
        ((TextView) findViewById(R.id.tv_detail_bus_no)).setText(busNo);
        ((TextView) findViewById(R.id.tv_detail_fee)).setText(fee);

        findViewById(R.id.btn_back_detail).setOnClickListener(v -> finish());
        findViewById(R.id.btn_confirm_booking).setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra("bus_no", busNo);
            intent.putExtra("fee", fee);
            startActivity(intent);
            finish();
        });

        // Setup Timeline with dynamic data
        RecyclerView rvTimeline = findViewById(R.id.rv_route_timeline);
        List<Stop> stops = new ArrayList<>();
        TimelineAdapter adapter = new TimelineAdapter(stops);
        rvTimeline.setAdapter(adapter);

        com.google.firebase.database.DatabaseReference routeRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("routes");
        routeRef.orderByChild("busNo").equalTo(busNo).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                stops.clear();
                for (com.google.firebase.database.DataSnapshot routeSnap : snapshot.getChildren()) {
                    // 1. Source
                    com.google.firebase.database.DataSnapshot source = routeSnap.child("source");
                    if (source.exists()) {
                        stops.add(new Stop(source.child("name").getValue(String.class), "07:00 AM", "Starting Point"));
                    }

                    // 2. Intermediate Waypoints
                    for (com.google.firebase.database.DataSnapshot stopSnap : routeSnap.child("stops").getChildren()) {
                        String sName = stopSnap.child("name").getValue(String.class);
                        if (sName != null) {
                            stops.add(new Stop(sName, "On Way", "Intermediate Stop"));
                        }
                    }

                    // 3. Destination
                    com.google.firebase.database.DataSnapshot dest = routeSnap.child("destination");
                    if (dest.exists()) {
                        stops.add(new Stop(dest.child("name").getValue(String.class), "08:30 AM", "Final Destination"));
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(RouteDetailActivity.this, "Failed to load route data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class Stop {
        String name, time, desc;
        Stop(String n, String t, String d) { name = n; time = t; desc = d; }
    }

    class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.VH> {
        List<Stop> list;
        TimelineAdapter(List<Stop> l) { list = l; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_stop_timeline, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            Stop s = list.get(p);
            h.name.setText(s.name);
            h.time.setText("Scheduled: " + s.time);
            h.desc.setText(s.desc);
            
            // Hide line for last item
            if (p == list.size() - 1) {
                h.line.setVisibility(View.GONE);
                h.dot.setBackgroundResource(R.drawable.ic_dot_inactive);
            } else {
                h.line.setVisibility(View.VISIBLE);
                h.dot.setBackgroundResource(R.drawable.ic_dot_active);
            }
        }
        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView name, time, desc; View line, dot;
            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tv_stop_name);
                time = v.findViewById(R.id.tv_stop_time);
                desc = v.findViewById(R.id.tv_stop_desc);
                line = v.findViewById(R.id.timeline_line);
                dot = v.findViewById(R.id.timeline_dot);
            }
        }
    }
}
