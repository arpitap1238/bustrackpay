package com.example.driverapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {


    private String driverId = "";
    private com.google.firebase.database.DatabaseReference driverRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        android.content.SharedPreferences pref = getActivity().getSharedPreferences("DriverPrefs", android.content.Context.MODE_PRIVATE);
        driverId = pref.getString("driverId", "");
        String name = pref.getString("name", "Driver Name");
        String phone = pref.getString("phone", "Not Set");
        String license = pref.getString("licenseNo", "Not Set");
        String busNo = pref.getString("busNo", "00");

        if (!driverId.isEmpty()) {
            driverRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("drivers").child(driverId);
        }

        // Bind Views
        android.widget.TextView tvName = view.findViewById(R.id.tv_profile_name);
        android.widget.TextView tvId = view.findViewById(R.id.tv_profile_id);
        android.widget.TextView tvPhone = view.findViewById(R.id.tv_profile_phone);
        android.widget.TextView tvLicense = view.findViewById(R.id.tv_profile_license);
        android.widget.TextView tvBus = view.findViewById(R.id.tv_profile_bus);
        android.widget.TextView tvReg = view.findViewById(R.id.tv_profile_reg);

        android.view.View tilName = view.findViewById(R.id.til_edit_name);
        android.view.View tilPhone = view.findViewById(R.id.til_edit_phone);
        android.widget.EditText etName = view.findViewById(R.id.et_edit_name);
        android.widget.EditText etPhone = view.findViewById(R.id.et_edit_phone);

        android.view.View layoutPId = view.findViewById(R.id.layout_view_id);
        android.view.View layoutPLicense = view.findViewById(R.id.layout_view_license);
        android.view.View layoutPPhone = view.findViewById(R.id.layout_view_phone);

        android.widget.Button btnEdit = view.findViewById(R.id.btn_edit_profile);
        android.widget.Button btnSave = view.findViewById(R.id.btn_save_profile);

        // Populate initial data
        if (tvName != null) tvName.setText(name);
        if (tvId != null) tvId.setText(driverId);
        if (tvPhone != null) tvPhone.setText(phone);
        if (tvLicense != null) tvLicense.setText(license);
        if (tvBus != null) tvBus.setText("Bus #" + busNo);
        if (tvReg != null) tvReg.setText("MH 09 AZ 4200");

        btnEdit.setOnClickListener(v -> {
            boolean isEditing = tilName.getVisibility() == View.VISIBLE;
            if (!isEditing) {
                // Enter Edit Mode
                tilName.setVisibility(View.VISIBLE);
                tilPhone.setVisibility(View.VISIBLE);
                btnSave.setVisibility(View.VISIBLE);
                
                layoutPId.setVisibility(View.GONE);
                layoutPPhone.setVisibility(View.GONE);
                layoutPLicense.setVisibility(View.GONE);
                
                etName.setText(tvName.getText());
                etPhone.setText(tvPhone.getText());
                btnEdit.setText("Cancel");
            } else {
                // Cancel Edit
                tilName.setVisibility(View.GONE);
                tilPhone.setVisibility(View.GONE);
                btnSave.setVisibility(View.GONE);
                
                layoutPId.setVisibility(View.VISIBLE);
                layoutPPhone.setVisibility(View.VISIBLE);
                layoutPLicense.setVisibility(View.VISIBLE);
                btnEdit.setText("Edit");
            }
        });

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (newName.isEmpty() || newPhone.isEmpty()) {
                android.widget.Toast.makeText(getContext(), "Fields cannot be empty", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // Sync with Firebase
            if (driverRef != null) {
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("name", newName);
                updates.put("phone", newPhone);
                driverRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    // Sync with Prefs
                    pref.edit().putString("name", newName).putString("phone", newPhone).apply();
                    
                    // Update UI
                    tvName.setText(newName);
                    tvPhone.setText(newPhone);
                    
                    // Exit Edit Mode
                    btnEdit.performClick();
                    android.widget.Toast.makeText(getContext(), "Profile Updated!", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });

        android.widget.Button btnLogout = view.findViewById(R.id.btn_logout_profile);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSharedPreferences("DriverPrefs", android.content.Context.MODE_PRIVATE).edit().clear().apply();
                    android.content.Intent intent = new android.content.Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
            });
        }

        return view;
    }
}
