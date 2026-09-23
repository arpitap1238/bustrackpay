package com.example.driverapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StudentListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rvStudents = findViewById(R.id.rv_students);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));

        // TODO: Sync to Firebase Realtime Database
        List<Student> students = new ArrayList<>();
        students.add(new Student("Rahul Sharma", "ENR-90182", "Paid", "Valid"));
        students.add(new Student("Priya Patel", "ENR-90183", "Pending", "Expired"));
        students.add(new Student("Amit Singh", "ENR-90184", "Paid", "Valid"));
        students.add(new Student("Neha Gupta", "ENR-90185", "Paid", "Valid"));
        students.add(new Student("Sahil Khan", "ENR-90186", "Pending", "Valid"));

        StudentAdapter adapter = new StudentAdapter(students);
        rvStudents.setAdapter(adapter);
    }
}
