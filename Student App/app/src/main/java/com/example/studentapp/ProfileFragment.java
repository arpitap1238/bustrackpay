package com.example.studentapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        android.widget.TextView tvName = view.findViewById(R.id.tv_name);
        android.widget.TextView tvEnrollment = view.findViewById(R.id.tv_enrollment_no);
        android.widget.TextView tvEmail = view.findViewById(R.id.tv_email);
        android.widget.TextView tvPhone = view.findViewById(R.id.tv_phone);

        android.content.SharedPreferences pref = getActivity().getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE);
        String enrollmentId = pref.getString("enrollmentId", "Unknown");
        String name = pref.getString("name", "Student Name");
        
        if (tvName != null) tvName.setText(name);
        if (tvEnrollment != null) tvEnrollment.setText("Enrollment No: " + enrollmentId);

        com.google.firebase.database.DatabaseReference ref = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("students").child(enrollmentId);
        ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sEmail = snapshot.child("email").getValue(String.class);
                    String sPhone = snapshot.child("phone").getValue(String.class);
                    String sBus = snapshot.child("busNo").getValue(String.class);
                    String sFees = snapshot.child("feeStatus").getValue(String.class);
                    String sPass = snapshot.child("passStatus").getValue(String.class);

                    if (tvEmail != null) tvEmail.setText(sEmail != null ? sEmail : "Not Configured");
                    if (tvPhone != null) tvPhone.setText(sPhone != null ? sPhone : "Not Configured");
                    
                    android.widget.TextView tvBus = view.findViewById(R.id.tv_profile_bus);
                    android.widget.TextView tvFees = view.findViewById(R.id.tv_profile_fees);
                    android.widget.TextView tvPass = view.findViewById(R.id.tv_profile_pass);

                    if (tvBus != null) tvBus.setText(sBus != null ? "Bus #" + sBus : "Not Assigned");
                    if (tvFees != null) {
                        tvFees.setText(sFees != null ? sFees : "Pending");
                        tvFees.setTextColor(android.graphics.Color.parseColor("Paid".equals(sFees) ? "#22C55E" : "#EF4444"));
                    }
                    if (tvPass != null) {
                        tvPass.setText(sPass != null ? sPass : "Invalid");
                        tvPass.setTextColor(android.graphics.Color.parseColor("Valid".equals(sPass) ? "#22C55E" : "#6B7280"));
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        });

        Button btnLogout = view.findViewById(R.id.btn_logout);
        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> {
            showEditProfileDialog(enrollmentId, tvEmail.getText().toString(), tvPhone.getText().toString());
        });

        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                android.content.SharedPreferences p = getActivity().getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE);
                p.edit().clear().apply();
                
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        return view;
    }

    private void showEditProfileDialog(String enrollmentId, String currentEmail, String currentPhone) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        final android.widget.EditText etEmail = new android.widget.EditText(getContext());
        etEmail.setHint("Email Address");
        etEmail.setText("Not Configured".equals(currentEmail) ? "" : currentEmail);
        etEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(etEmail);

        final android.widget.EditText etPhone = new android.widget.EditText(getContext());
        etPhone.setHint("Mobile Number");
        etPhone.setText("Not Configured".equals(currentPhone) ? "" : currentPhone);
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 30;
        etPhone.setLayoutParams(params);
        layout.addView(etPhone);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext(), R.style.CustomDialog)
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("SAVE", (dialog, which) -> {
                    String newEmail = etEmail.getText().toString();
                    String newPhone = etPhone.getText().toString();
                    
                    if (newEmail.isEmpty() || newPhone.isEmpty()) {
                        android.widget.Toast.makeText(getContext(), "Please fill all fields", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("email", newEmail);
                    updates.put("phone", newPhone);

                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("students")
                            .child(enrollmentId).updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded()) android.widget.Toast.makeText(getContext(), "Profile Updated", android.widget.Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }
}
