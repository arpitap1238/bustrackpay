package com.example.driverapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView tvHeaderTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        tvHeaderTitle = findViewById(R.id.tv_header_title);

        // Header Buttons
        findViewById(R.id.btn_menu).setOnClickListener(v -> drawerLayout.open());
        findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            updateNavState(new NotificationsFragment(), null, "Notifications");
        });

        // Side Drawer Click Listeners
        NavigationView sideNav = findViewById(R.id.nav_view_side);
        sideNav.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.side_home) updateNavState(new DashboardFragment(), findViewById(R.id.custom_nav_home), "Dashboard");
            else if (id == R.id.side_routes) updateNavState(new RoutesFragment(), findViewById(R.id.custom_nav_routes), "Routes");
            else if (id == R.id.side_students) updateNavState(new StudentsFragment(), findViewById(R.id.custom_nav_students), "Students");
            else if (id == R.id.side_profile) updateNavState(new ProfileFragment(), findViewById(R.id.custom_nav_profile), "Profile");
            else if (id == R.id.side_notifications) updateNavState(new NotificationsFragment(), null, "Notifications");
            else if (id == R.id.side_support) updateNavState(new SupportFragment(), null, "Help & Support");
            else if (id == R.id.side_logout) {
                getSharedPreferences("DriverPrefs", android.content.Context.MODE_PRIVATE).edit().clear().apply();
                startActivity(new android.content.Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
            
            drawerLayout.close();
            return true;
        });

        // Custom Bottom Nav Click Listeners
        ImageView navHome = findViewById(R.id.custom_nav_home);
        ImageView navRoutes = findViewById(R.id.custom_nav_routes);
        ImageView navStudents = findViewById(R.id.custom_nav_students);
        ImageView navProfile = findViewById(R.id.custom_nav_profile);

        navHome.setOnClickListener(v -> updateNavState(new DashboardFragment(), navHome, "Dashboard"));
        navRoutes.setOnClickListener(v -> updateNavState(new RoutesFragment(), navRoutes, "Routes"));
        navStudents.setOnClickListener(v -> updateNavState(new StudentsFragment(), navStudents, "Students"));
        navProfile.setOnClickListener(v -> updateNavState(new ProfileFragment(), navProfile, "Profile"));

        // Center FAB Action - Quick Map/Route Access
        findViewById(R.id.fab_center).setOnClickListener(v -> {
            updateNavState(new RoutesFragment(), navRoutes, "Duty Hub");
            // Highlight the FAB effect
            v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).withEndAction(() -> 
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            ).start();
        });

        // Initialize with default fragment
        updateNavState(new DashboardFragment(), findViewById(R.id.custom_nav_home), "Home");

        setupHeader();
    }

    private void setupHeader() {
        android.content.SharedPreferences pref = getSharedPreferences("DriverPrefs", android.content.Context.MODE_PRIVATE);
        String name = pref.getString("name", "Driver");
        String driverId = pref.getString("driverId", "N/A");

        com.google.android.material.navigation.NavigationView sideNav = findViewById(R.id.nav_view_side);
        View headerView = sideNav.getHeaderView(0);
        android.widget.TextView tvName = headerView.findViewById(R.id.tv_driver_name_side);
        android.widget.TextView tvId = headerView.findViewById(R.id.tv_driver_id_side);
        
        if (tvName != null) tvName.setText(name);
        if (tvId != null) tvId.setText(driverId);
    }

    public void updateNavState(Fragment fragment, ImageView activeIcon, String title) {
        if (tvHeaderTitle != null) tvHeaderTitle.setText(title);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
        
        resetNavTints();
        
        if (activeIcon != null) {
            activeIcon.setColorFilter(Color.parseColor("#0F67FD"));
            activeIcon.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start();

            // Sliding Indicator Animation
            View indicator = findViewById(R.id.nav_indicator);
            if (indicator != null) {
                indicator.setVisibility(View.VISIBLE);
                float targetX = activeIcon.getX() + (activeIcon.getWidth() / 2f) - (indicator.getWidth() / 2f);
                indicator.animate().translationX(targetX).setDuration(300).start();
            }
        } else {
            View indicator = findViewById(R.id.nav_indicator);
            if (indicator != null) indicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (!(currentFragment instanceof DashboardFragment)) {
            updateNavState(new DashboardFragment(), findViewById(R.id.custom_nav_home), "Dashboard");
        } else {
            super.onBackPressed();
        }
    }

    private void resetNavTints() {
        int inactiveColor = Color.parseColor("#94A3B8");
        int[] navIds = {R.id.custom_nav_home, R.id.custom_nav_routes, R.id.custom_nav_students, R.id.custom_nav_profile};
        for (int id : navIds) {
            ImageView icon = findViewById(id);
            if (icon != null) {
                icon.setColorFilter(inactiveColor);
                icon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        }
    }
}