package com.example.driverapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

public class StudentsFragment extends Fragment {
    
    private RecyclerView rvStudents;
    private List<Student> fullStudentList = new ArrayList<>();
    private List<Student> filteredStudentList = new ArrayList<>();
    private StudentAdapter adapter;
    private String busNo = "";
    private EditText etSearch;
    private android.widget.ImageView ivClearSearch;
    private ChipGroup chipGroup;
    private String currentSearch = "";
    private int currentFilterId = R.id.chip_all;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_students, container, false);

        SharedPreferences pref = getActivity().getSharedPreferences("DriverPrefs", Context.MODE_PRIVATE);
        busNo = pref.getString("busNo", "");

        rvStudents = view.findViewById(R.id.rv_students);
        rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new StudentAdapter(filteredStudentList);
        rvStudents.setAdapter(adapter);

        etSearch = view.findViewById(R.id.et_search);
        ivClearSearch = view.findViewById(R.id.iv_clear_search);
        chipGroup = view.findViewById(R.id.chip_group_filter);

        setupListeners();
        loadStudents();

        return view;
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().toLowerCase();
                ivClearSearch.setVisibility(currentSearch.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        ivClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            currentSearch = "";
            applyFilters();
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                currentFilterId = checkedIds.get(0);
                applyFilters();
            }
        });
    }

    private void applyFilters() {
        filteredStudentList.clear();
        for (Student student : fullStudentList) {
            boolean matchesSearch = student.name.toLowerCase().contains(currentSearch) || 
                                     student.enrollmentId.toLowerCase().contains(currentSearch);
            
            boolean matchesFilter = true;
            if (currentFilterId == R.id.chip_unpaid) {
                matchesFilter = "Pending".equalsIgnoreCase(student.feeStatus);
            } else if (currentFilterId == R.id.chip_invalid) {
                matchesFilter = !"Valid".equalsIgnoreCase(student.passStatus);
            }

            if (matchesSearch && matchesFilter) {
                filteredStudentList.add(student);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadStudents() {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("students");
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullStudentList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Student student = postSnapshot.getValue(Student.class);
                    if (student != null && busNo.equals(student.busNo)) {
                        fullStudentList.add(student);
                    }
                }
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StudentsFragment", "Load failed", error.toException());
            }
        });
    }
}
