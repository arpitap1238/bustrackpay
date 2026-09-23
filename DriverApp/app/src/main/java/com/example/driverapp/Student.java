package com.example.driverapp;

public class Student {
    public String name;
    public String enrollmentId;
    public String email;
    public String phone;
    public String pickupStop; // Matches Firebase key
    public String busNo;
    public String feeStatus; // "Paid" or "Pending"
    public String passStatus; // "Valid" or "Expired"
    public String feeSem1; // "Paid" or "Pending"
    public String feeSem2; // "Paid" or "Pending"

    public Student() {
        // Required for Firebase
    }

    public Student(String name, String enrollmentId, String feeStatus, String passStatus) {
        this.name = name;
        this.enrollmentId = enrollmentId;
        this.feeStatus = feeStatus;
        this.passStatus = passStatus;
    }
}
