package com.google.android.sys.security;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stub: kiểm tra / hiển thị trạng thái kết nối server (LAN/Remote).
 */
public class ServerConnectionChecker {
    private final Context context;
    private final SharedPreferences prefs;

    public ServerConnectionChecker(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences("sys_security_prefs", Context.MODE_PRIVATE);
    }

    public String getStatusString() {
        String host = prefs.getString("server_host", null);
        String port = prefs.getString("server_port", null);
        if (host != null && !host.isEmpty()) {
            return "Server: " + host + (port != null ? ":" + port : "");
        }
        return "Service running";
    }

    public String getPreferredMode() {
        if (prefs.getBoolean("force_remote", false)) return "REMOTE";
        String host = prefs.getString("server_host", "");
        if (host != null && !host.isEmpty() && !host.startsWith("192.168.")) return "REMOTE";
        return "LAN";
    }
}
