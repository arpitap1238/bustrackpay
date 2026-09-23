package com.example.studentapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class RoutesFragment extends Fragment {

    private RecyclerView rvRoutes;
    private List<BusRoute> allRoutes = new ArrayList<>();
    private List<BusRoute> filteredRoutes = new ArrayList<>();
    private String studentBusNo = "";
    private DatabaseReference mDatabase;

    private View cardAssigned, layoutDiscovery, layoutNoPass;
    private TextView tvTitle, tvAssignedNo, tvAssignedRoute, tvAssignedStart, tvAssignedEnd;

    private String studentPickupStop = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routes, container, false);
        
        SharedPreferences pref = getActivity().getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE);
        studentBusNo = pref.getString("busNo", "");
        studentPickupStop = pref.getString("pickupStop", "");

        rvRoutes = view.findViewById(R.id.rv_routes);
        rvRoutes.setAdapter(new RoutesAdapter(filteredRoutes));

        cardAssigned = view.findViewById(R.id.card_assigned_bus);
        layoutDiscovery = view.findViewById(R.id.layout_discovery);
        
        tvTitle = view.findViewById(R.id.tv_routes_title);
        tvAssignedNo = view.findViewById(R.id.tv_assigned_bus_no);
        tvAssignedRoute = view.findViewById(R.id.tv_assigned_route_name);
        tvAssignedStart = view.findViewById(R.id.tv_assigned_start);
        tvAssignedEnd = view.findViewById(R.id.tv_assigned_end);

        updateUIState();

        view.findViewById(R.id.btn_change_pickup).setOnClickListener(v -> showPickupSelection());

        mDatabase = FirebaseDatabase.getInstance().getReference("routes");
        loadRoutes();
        loadStudentExtras();

        // Search Implementation
        android.widget.EditText etSearch = view.findViewById(R.id.et_search_routes);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchRoutes(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        return view;
    }

    private void loadStudentExtras() {
        String enrollmentId = getActivity().getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE)
                .getString("enrollmentId", "");
        if (enrollmentId.isEmpty()) return;

        FirebaseDatabase.getInstance().getReference("students").child(enrollmentId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists() || !isAdded() || getActivity() == null) return;
                    
                    String newBusNo = snapshot.child("busNo").getValue(String.class);
                    if (newBusNo != null && !newBusNo.equals(studentBusNo)) {
                        studentBusNo = newBusNo;
                        SharedPreferences.Editor editor = getActivity().getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE).edit();
                        editor.putString("busNo", studentBusNo);
                        editor.apply();
                        updateUIState();
                    }

                    studentPickupStop = snapshot.child("pickupStop").getValue(String.class);
                    if (studentPickupStop != null && !studentPickupStop.isEmpty()) {
                        SharedPreferences.Editor editor = getActivity().getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE).edit();
                        editor.putString("pickupStop", studentPickupStop);
                        editor.apply();
                        tvAssignedStart.setText(studentPickupStop);
                    }
                    tvAssignedEnd.setText("College Campus");
                    updateUIState();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void showPickupSelection() {
        BusRoute studentRoute = null;
        for (BusRoute r : allRoutes) {
            if (r.busNo.equals(studentBusNo)) {
                studentRoute = r;
                break;
            }
        }

        if (studentRoute == null || studentRoute.stops == null) {
            android.widget.Toast.makeText(getContext(), "Route data not available", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // We only allow selection from stops (boarding points). Last stop is College.
        final String[] stops = studentRoute.stops.toArray(new String[0]);
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
            .setTitle("Select Pickup Point")
            .setItems(stops, (dialog, which) -> updatePickupStop(stops[which]))
            .show();
    }

    private void updatePickupStop(String stopName) {
        String enrollmentId = getActivity().getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE)
                .getString("enrollmentId", "");
        if (enrollmentId.isEmpty()) return;

        FirebaseDatabase.getInstance().getReference("students").child(enrollmentId)
            .child("pickupStop").setValue(stopName)
            .addOnSuccessListener(aVoid -> {
                studentPickupStop = stopName;
                tvAssignedStart.setText(stopName);
                android.widget.Toast.makeText(getContext(), "Pickup Stop Updated", android.widget.Toast.LENGTH_SHORT).show();
            });
    }

    private void updateUIState() {
        android.util.Log.d("RoutesFragment", "Updating UI State. studentBusNo: [" + studentBusNo + "], allRoutes size: " + allRoutes.size());
        
        String cleanBusNo = studentBusNo != null ? studentBusNo.trim().toUpperCase() : "";
        boolean looksAssigned = !cleanBusNo.isEmpty() && 
                             !cleanBusNo.equals("NOT ASSIGNED") && 
                             !cleanBusNo.equals("BUS-NOT ASSIGNED") && 
                             !cleanBusNo.equals("..");

        BusRoute matchedRoute = null;
        if (looksAssigned) {
            for (BusRoute r : allRoutes) {
                if (r.busNo != null && (r.busNo.equalsIgnoreCase(studentBusNo) || r.busNo.equalsIgnoreCase(cleanBusNo))) {
                    matchedRoute = r;
                    break;
                }
            }
        }

        boolean isTrulyAssigned = looksAssigned && matchedRoute != null;
        
        if (tvTitle == null) return; // Defensive check
        if (isTrulyAssigned) {
            tvTitle.setText("Your Assigned Pass");
            cardAssigned.setVisibility(View.VISIBLE);
            layoutDiscovery.setVisibility(View.GONE);
            tvAssignedNo.setText("BUS #" + matchedRoute.busNo);
            tvAssignedRoute.setText(matchedRoute.name);
            tvAssignedStart.setText(studentPickupStop != null && !studentPickupStop.isEmpty() ? studentPickupStop : matchedRoute.startStop);
            tvAssignedEnd.setText("College Campus");
        } else {
            tvTitle.setText("Select Your Route");
            cardAssigned.setVisibility(View.GONE);
            layoutDiscovery.setVisibility(View.VISIBLE);
            searchRoutes(""); // Ensure list is refreshed
        }
    }

    private void loadRoutes() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allRoutes.clear();
                BusRoute studentRoute = null;
                
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    try {
                        BusRoute route = new BusRoute();
                        Object bNoVal = postSnapshot.child("busNo").getValue();
                        route.busNo = bNoVal != null ? String.valueOf(bNoVal).trim() : "";
                        route.name = postSnapshot.child("name").getValue(String.class);
                        if (route.name == null) route.name = "Unnamed Route";

                        List<String> stopNames = new ArrayList<>();
                        DataSnapshot source = postSnapshot.child("source");
                        if (source.exists()) {
                            route.startStop = source.child("name").getValue(String.class);
                            if (route.startStop != null) stopNames.add(route.startStop);
                        }

                        for (DataSnapshot stopSnap : postSnapshot.child("stops").getChildren()) {
                            String sName = stopSnap.child("name").getValue(String.class);
                            if (sName != null) stopNames.add(sName);
                        }
                        
                        DataSnapshot dest = postSnapshot.child("destination");
                        if (dest.exists()) {
                            route.endStop = dest.child("name").getValue(String.class);
                            if (route.endStop != null) stopNames.add(route.endStop);
                        }

                        route.stops = stopNames;
                        if (route.busNo != null && route.busNo.equals(studentBusNo)) {
                            studentRoute = route;
                        }
                        allRoutes.add(route);
                    } catch (Exception e) {
                        Log.e("Routes", "Error parsing route: " + postSnapshot.getKey(), e);
                    }
                }

                android.util.Log.d("RoutesFragment", "Total Routes Loaded: " + allRoutes.size());

                if (studentRoute != null) {
                    tvAssignedRoute.setText(studentRoute.name);
                    tvAssignedStart.setText(studentRoute.startStop);
                    tvAssignedEnd.setText(studentRoute.endStop);
                }

                searchRoutes(""); // Initial load
                updateUIState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Routes", "Failed to load routes", error.toException());
            }
        });
    }

    private void searchRoutes(String query) {
        filteredRoutes.clear();
        for (BusRoute r : allRoutes) {
            boolean matchesSearch = r.name.toLowerCase().contains(query.toLowerCase()) || 
                                   (r.busNo != null && r.busNo.contains(query));
            if (matchesSearch) {
                filteredRoutes.add(r);
            }
        }
        // Sort to put student's bus first
        java.util.Collections.sort(filteredRoutes, (a, b) -> {
            if (a.busNo.equals(studentBusNo)) return -1;
            if (b.busNo.equals(studentBusNo)) return 1;
            return 0;
        });

        if (rvRoutes.getAdapter() != null) {
            rvRoutes.getAdapter().notifyDataSetChanged();
        }
    }

    public static class BusRoute {
        public String name, busNo, startStop, endStop, startTime = "07:30 AM", endTime = "08:30 AM", fee = "₹15,000", type = "AC BUS";
        public List<String> stops;
        public BusRoute() {}
    }

    class RoutesAdapter extends RecyclerView.Adapter<RoutesAdapter.RouteViewHolder> {
        List<BusRoute> routes;
        RoutesAdapter(List<BusRoute> r) { routes = r; }
        
        @NonNull
        @Override
        public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
            return new RouteViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
            BusRoute r = routes.get(position);
            holder.tvName.setText(r.name);
            holder.tvBusNo.setText("Bus #" + r.busNo);
            holder.tvStart.setText(r.startStop);
            holder.tvEnd.setText(r.endStop);
            holder.tvTimeStart.setText(r.startTime);
            holder.tvTimeEnd.setText(r.endTime);
            holder.tvFee.setText(r.fee);

            if (r.busNo.equals(studentBusNo)) {
                holder.itemView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2FE"))); // Light cyan for assigned bus
            } else {
                holder.itemView.setBackgroundTintList(null);
            }

            holder.btnEnroll.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), RouteDetailActivity.class);
                intent.putExtra("route_name", r.name);
                intent.putExtra("bus_no", r.busNo);
                intent.putExtra("fee", r.fee);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return routes.size();
        }

        class RouteViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvBusNo, tvStart, tvEnd, tvTimeStart, tvTimeEnd, tvFee;
            View btnEnroll;
            RouteViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_route_name);
                tvBusNo = itemView.findViewById(R.id.tv_bus_number);
                tvStart = itemView.findViewById(R.id.tv_stop_start);
                tvEnd = itemView.findViewById(R.id.tv_stop_end);
                tvTimeStart = itemView.findViewById(R.id.tv_time_start);
                tvTimeEnd = itemView.findViewById(R.id.tv_time_end);
                tvFee = itemView.findViewById(R.id.tv_fee_amount);
                btnEnroll = itemView.findViewById(R.id.btn_enroll_route);
            }
        }
    }
}
