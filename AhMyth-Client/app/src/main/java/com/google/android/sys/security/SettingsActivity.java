package com.google.android.sys.security;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import android.view.Gravity;
import android.util.TypedValue;

/**
 * Cài đặt: Host + Port server (để kết nối từ xa / LAN).
 */
public class SettingsActivity extends Activity {
    private EditText editHost, editPort;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("sys_security_prefs", MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(24), dp(24), dp(24));
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Server settings");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        layout.addView(title);

        TextView lHost = new TextView(this);
        lHost.setText("Host (IP or domain):");
        layout.addView(lHost);
        editHost = new EditText(this);
        editHost.setHint("e.g. sys_securityserver.duckdns.org or 192.168.1.2");
        editHost.setText(prefs.getString("server_host", ""));
        editHost.setMinWidth(400);
        layout.addView(editHost);

        TextView lPort = new TextView(this);
        lPort.setText("Port:");
        layout.addView(lPort);
        editPort = new EditText(this);
        editPort.setHint("42474");
        editPort.setText(prefs.getString("server_port", "42474"));
        editPort.setMinWidth(200);
        layout.addView(editPort);

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> saveAndFinish());
        layout.addView(save);

        setContentView(layout);
    }

    private int dp(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (dp * d);
    }

    private void saveAndFinish() {
        String host = editHost.getText() != null ? editHost.getText().toString().trim() : "";
        String port = editPort.getText() != null ? editPort.getText().toString().trim() : "42474";
        prefs.edit()
            .putString("server_host", host)
            .putString("server_port", port.isEmpty() ? "42474" : port)
            .apply();
        finish();
    }
}
