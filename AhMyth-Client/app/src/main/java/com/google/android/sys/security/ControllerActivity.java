package com.google.android.sys.security;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Placeholder: màn hình Controller (Phone Control).
 */
public class ControllerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Controller");
        tv.setPadding(48, 48, 48, 48);
        setContentView(tv);
    }
}
