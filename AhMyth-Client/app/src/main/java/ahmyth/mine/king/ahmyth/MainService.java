package ahmyth.mine.king.ahmyth;

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
            String channelId = "ahmyth_service_channel";
            String channelName = getString(R.string.service_channel_name);
            // Tạo channel với IMPORTANCE_NONE để ẩn notification
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, channelName, android.app.NotificationManager.IMPORTANCE_NONE);
            channel.setLightColor(android.graphics.Color.BLUE);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_SECRET);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            
            android.app.NotificationManager manager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                // Tạo notification ẩn, nhưng thêm action mở Settings
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                PendingIntent pendingSettings = PendingIntent.getActivity(this, 0, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                android.app.Notification.Builder nb = new android.app.Notification.Builder(this, channelId)
                    .setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.service_notification_title))
                    .setContentText(getString(R.string.service_notification_active))
                    .setPriority(android.app.Notification.PRIORITY_MIN)
                    .setCategory(android.app.Notification.CATEGORY_SERVICE)
                    .setVibrate(new long[]{})
                    .setSound(null)
                    .setVisibility(android.app.Notification.VISIBILITY_SECRET)
                    .addAction(android.R.drawable.ic_menu_manage, "Settings", pendingSettings);

                android.app.Notification notification = nb.build();
                startForeground(101, notification);
            }
        } else {
             // For older versions
        }

        contextOfApplication = this;
        ConnectionManager.startAsync(this);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public static Context getContextOfApplication()
    {
        return contextOfApplication;
    }


}
