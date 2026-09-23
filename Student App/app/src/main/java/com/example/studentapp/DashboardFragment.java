package com.example.studentapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        View cardFeatured = view.findViewById(R.id.card_featured);
        View btnDetail = view.findViewById(R.id.btn_view_detail);
        View cardRoutes = view.findViewById(R.id.card_routes);
        View cardTrack = view.findViewById(R.id.card_track_grid);
        View cardFees = view.findViewById(R.id.card_fees);
        View cardProfile = view.findViewById(R.id.card_profile);

        if (getActivity() != null) {
            android.content.SharedPreferences pref = getActivity().getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE);
            String enrollmentId = pref.getString("enrollmentId", "");
            
            if (!enrollmentId.isEmpty()) {
                com.google.firebase.database.DatabaseReference studentRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("students").child(enrollmentId);
                studentRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String busNo = snapshot.child("busNo").getValue(String.class);
                            String feeStatus = snapshot.child("feeStatus").getValue(String.class);
                            
                            android.widget.TextView tvStatus = view.findViewById(R.id.tv_status_main);
                            if (tvStatus != null) tvStatus.setText("Bus #" + (busNo != null ? busNo : "--") + " Tracking");
                            
                            android.widget.TextView tvGridHint = view.findViewById(R.id.tv_grid_bus_hint);
                            if (tvGridHint != null) tvGridHint.setText("Bus #" + (busNo != null ? busNo : "--") + " Active");
                            
                            String feeSem1 = snapshot.child("feeSem1").getValue(String.class);
                            String feeSem2 = snapshot.child("feeSem2").getValue(String.class);
                            
                            android.widget.TextView tvFeesHint = view.findViewById(R.id.tv_fees_hint);
                            if (tvFeesHint != null) {
                                if ("Paid".equals(feeSem1) && "Paid".equals(feeSem2)) {
                                    tvFeesHint.setText("Full Year: Paid");
                                } else if ("Paid".equals(feeSem1)) {
                                    tvFeesHint.setText("Sem 1: Paid • Sem 2: Due");
                                } else {
                                    tvFeesHint.setText("Sem 1: Due");
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });
            }
        }

        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            
            cardFeatured.setOnClickListener(v -> main.updateNavState(new TrackingFragment(), null, "Tracking"));
            btnDetail.setOnClickListener(v -> main.updateNavState(new TrackingFragment(), null, "Tracking"));
            cardTrack.setOnClickListener(v -> main.updateNavState(new TrackingFragment(), null, "Tracking"));
            
            cardRoutes.setOnClickListener(v -> 
                main.updateNavState(new RoutesFragment(), main.findViewById(R.id.custom_nav_routes), "Routes"));
            
            cardFees.setOnClickListener(v -> 
                main.updateNavState(new FeesFragment(), main.findViewById(R.id.custom_nav_fees), "Fees"));
            
            cardProfile.setOnClickListener(v -> 
                main.updateNavState(new ProfileFragment(), main.findViewById(R.id.custom_nav_profile), "Profile"));
        }

        return view;
    }
}
