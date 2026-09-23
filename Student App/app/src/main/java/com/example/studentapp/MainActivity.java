package com.example.studentapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        
        // Header Buttons
        findViewById(R.id.btn_menu).setOnClickListener(v -> drawerLayout.open());
        findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            updateNavState(new NotificationFragment(), null, "Notifications");
        });

        // Side Drawer Click Listeners
        com.google.android.material.navigation.NavigationView sideNav = findViewById(R.id.nav_view_side);
        sideNav.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.side_home) updateNavState(new DashboardFragment(), findViewById(R.id.custom_nav_home), "Home");
            else if (id == R.id.side_tracking) updateNavState(new TrackingFragment(), null, "Tracking");
            else if (id == R.id.side_routes) updateNavState(new RoutesFragment(), findViewById(R.id.custom_nav_routes), "Routes");
            else if (id == R.id.side_fees) updateNavState(new FeesFragment(), findViewById(R.id.custom_nav_fees), "Fees");
            else if (id == R.id.side_profile) updateNavState(new ProfileFragment(), findViewById(R.id.custom_nav_profile), "Profile");
            else if (id == R.id.side_notifications) updateNavState(new NotificationFragment(), null, "Notifications");
            else if (id == R.id.side_support) updateNavState(new SupportFragment(), null, "Help & Support");
            
            drawerLayout.close();
            return true;
        });

        // Custom Bottom Nav Click Listeners
        ImageView navHome = findViewById(R.id.custom_nav_home);
        ImageView navRoutes = findViewById(R.id.custom_nav_routes);
        ImageView navFees = findViewById(R.id.custom_nav_fees);
        ImageView navProfile = findViewById(R.id.custom_nav_profile);

        navHome.setOnClickListener(v -> updateNavState(new DashboardFragment(), navHome, "Home"));
        navRoutes.setOnClickListener(v -> updateNavState(new RoutesFragment(), navRoutes, "Routes"));
        navFees.setOnClickListener(v -> updateNavState(new FeesFragment(), navFees, "Fees"));
        navProfile.setOnClickListener(v -> updateNavState(new ProfileFragment(), navProfile, "Profile"));

        // Center FAB Action
        findViewById(R.id.fab_center).setOnClickListener(v -> {
            updateNavState(new TrackingFragment(), null, "Tracking");
            // FAB Pop Animation
            v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(300).start();
        });

        // Initialize with default fragment
        updateNavState(new DashboardFragment(), navHome, "Home");

        setupHeader();
    }

    private void setupHeader() {
        android.content.SharedPreferences pref = getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE);
        String name = pref.getString("name", "Student");
        String enrollmentId = pref.getString("enrollmentId", "N/A");

        com.google.android.material.navigation.NavigationView sideNav = findViewById(R.id.nav_view_side);
        View headerView = sideNav.getHeaderView(0);
        android.widget.TextView tvName = headerView.findViewById(R.id.tv_user_name_side);
        android.widget.TextView tvId = headerView.findViewById(R.id.tv_student_id_side); // Ensure ID matches or update layout
        
        if (tvName != null) tvName.setText(name);
        if (tvId != null) tvId.setText(enrollmentId);
    }

    public void updateNavState(Fragment fragment, ImageView activeIcon, String title) {
        // Update Title
        android.widget.TextView tvTitle = findViewById(R.id.tv_header_title);
        if (tvTitle != null) tvTitle.setText(title);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_up_pop, R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
        
        resetNavTints();
        
        if (activeIcon != null) {
            activeIcon.setColorFilter(android.graphics.Color.parseColor("#266FEF"));
            // Icon Pop Animation
            activeIcon.animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).start();

            // Sliding Indicator Animation
            View indicator = findViewById(R.id.nav_indicator);
            if (indicator != null) {
                indicator.setVisibility(View.VISIBLE);
                float targetX = activeIcon.getX() + (activeIcon.getWidth() / 2f) - (indicator.getWidth() / 2f);
                indicator.animate().translationX(targetX).setDuration(300).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            }
            
            // Settle FAB if nav item is clicked
            findViewById(R.id.fab_center).animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        } else {
            // Hide indicator when jumping to center FAB
            View indicator = findViewById(R.id.nav_indicator);
            if (indicator != null) indicator.setVisibility(View.GONE);
        }
    }
//
//    @Override
//    public void onBackPressed() {
//        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
//        if (!(currentFragment instanceof DashboardFragment)) {
//            updateNavState(new DashboardFragment(), findViewById(R.id.custom_nav_home), "Home");
//        } else {
//            super.onBackPressed();
//        }
//    }

    public void resetNavTints() {
        int inactiveColor = android.graphics.Color.parseColor("#94A3B8");
        int[] navIds = {R.id.custom_nav_home, R.id.custom_nav_routes, R.id.custom_nav_fees, R.id.custom_nav_profile};
        for (int id : navIds) {
            ImageView icon = findViewById(id);
            if (icon != null) {
                icon.setColorFilter(inactiveColor);
                icon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        }
    }
}