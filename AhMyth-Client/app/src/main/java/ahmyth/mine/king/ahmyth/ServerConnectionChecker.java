package ahmyth.mine.king.ahmyth;

import android.content.SharedPreferences;
import android.content.Context;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ServerConnectionChecker {
    private SharedPreferences prefs;
    private Context context;

    public ServerConnectionChecker(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("ahmyth", Context.MODE_PRIVATE);
    }

    /**
     * Check if LAN server is reachable
     */
    public boolean isLanAvailable() {
        String lanIp = prefs.getString("lan_ip", "");
        String port = prefs.getString("server_port", "42474");
        
        if (lanIp.isEmpty()) {
            return false;
        }
        
        return isServerReachable(lanIp, port);
    }

    /**
     * Check if Remote server is reachable
     */
    public boolean isRemoteAvailable() {
        String remoteHost = prefs.getString("server_host", "");
        String port = prefs.getString("server_port", "443");
        
        if (remoteHost.isEmpty()) {
            return false;
        }
        
        return isServerReachable(remoteHost, port);
    }

    /**
     * Determine which mode to use (LAN takes priority if available)
     */
    public String getPreferredMode() {
        if (isLanAvailable()) {
            return "LAN";
        } else if (isRemoteAvailable()) {
            return "REMOTE";
        }
        return "OFFLINE";
    }

    /**
     * Get the server URL based on preferred mode
     */
    public String getServerUrl() {
        String mode = getPreferredMode();
        String lanIp = prefs.getString("lan_ip", "");
        String remoteHost = prefs.getString("server_host", "");
        String port = prefs.getString("server_port", "42474");
        
        if ("LAN".equals(mode) && !lanIp.isEmpty()) {
            return "http://" + lanIp + ":" + port;
        } else if ("REMOTE".equals(mode) && !remoteHost.isEmpty()) {
            return "https://" + remoteHost + ":443";
        }
        
        return null;
    }

    /**
     * Check if a specific host:port is reachable
     */
    private boolean isServerReachable(String host, String port) {
        try {
            int portNum = Integer.parseInt(port);
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, portNum), 3000); // 3 second timeout
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get current connection status string
     */
    public String getStatusString() {
        String mode = getPreferredMode();
        switch (mode) {
            case "LAN":
                return "🟢 LAN Connected (Fast)";
            case "REMOTE":
                return "🟡 Remote (NGrok)";
            case "OFFLINE":
                return "🔴 Offline - Configure in Settings";
            default:
                return "⚪ Unknown";
        }
    }
}
