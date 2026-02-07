package com.google.android.sys.security;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Placeholder: màn hình Dashboard (PC Monitor).
 */
public class DashboardActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Dashboard");
        tv.setPadding(48, 48, 48, 48);
        setContentView(tv);
    }
}
