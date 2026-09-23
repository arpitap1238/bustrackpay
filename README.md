# 🚌 BusTrackPay

> A smart bus transportation management system designed to simplify student bus enrollment, fee management, live tracking, and communication between students, drivers, and administrators.

---

## 📌 Project Overview

**BusTrackPay** is an integrated bus transportation management system developed to make college/school bus services more efficient and transparent.

The system consists of three major applications:

- 📱 **Student App** – For students to enroll in routes, check fee status, track buses, and receive notifications.
- 🚍 **Driver App** – For drivers to share live location, manage assigned routes, and communicate important updates.
- 🖥️ **Admin Panel** – For administrators to manage students, routes, drivers, payments, and announcements.

The project focuses on reducing manual work and providing real-time information to students and administrators.

---

## ✨ Key Features

### 👨‍🎓 Student App

- 🔐 Student login and authentication
- 🚌 Bus route enrollment
- 💰 Bus fee status
- 📍 Live bus tracking
- 📢 Important announcements
- 👤 Student account management
- 🆘 Help desk / support
- 📋 Bus and route information

### 🚍 Driver App

- 🔐 Driver login
- 📍 Live GPS location sharing
- 🚌 Assigned route management
- 👥 Student/route information
- 💰 Payment-related information
- 🔔 Alerts and notifications
- 🆘 Help/support functionality

### 🖥️ Admin Panel

- 👨‍🎓 Student management
- 🚍 Driver management
- 🚌 Route management
- 💰 Fee/payment management
- 📍 Bus monitoring
- 📢 Broadcast announcements
- 🔄 Update and manage transportation information

---

## 🏗️ System Architecture

```text
                    ┌─────────────────────┐
                    │    Admin Panel      │
                    │   HTML / CSS / JS   │
                    └──────────┬──────────┘
                               │
                               │
                    ┌──────────▼──────────┐
                    │      Firebase       │
                    │   Realtime Database │
                    └───────┬───────┬──────┘
                            │       │
                 ┌──────────┘       └──────────┐
                 │                             │
        ┌────────▼─────────┐          ┌───────▼────────┐
        │   Student App    │          │   Driver App   │
        │   Android        │          │   Android      │
        │   Java + XML     │          │   Java + XML   │
        └──────────────────┘          └────────────────┘
