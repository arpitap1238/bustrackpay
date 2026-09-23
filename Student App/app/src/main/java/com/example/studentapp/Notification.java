package com.example.studentapp;

public class Notification {
    public String title;
    public String message;
    public String target;
    public long timestamp;
    public String sender;

    public Notification() {} // Required for Firebase

    public Notification(String title, String message, String target, long timestamp, String sender) {
        this.title = title;
        this.message = message;
        this.target = target;
        this.timestamp = timestamp;
        this.sender = sender;
    }
}
