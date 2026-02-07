package ahmyth.mine.king.ahmyth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

public class DashboardActivity extends Activity {
    private WebView webView;
    private ServerConnectionChecker checker;
    private String currentMode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        checker = new ServerConnectionChecker(this);
        
        webView = findViewById(R.id.webview);
        setupWebView();
        loadDashboard();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Try fallback mode if current mode fails
                if ("LAN".equals(currentMode)) {
                    Toast.makeText(DashboardActivity.this, "LAN failed, trying Remote...", Toast.LENGTH_SHORT).show();
                    loadRemote();
                } else {
                    Toast.makeText(DashboardActivity.this, "Connection failed: " + description, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadDashboard() {
        currentMode = checker.getPreferredMode();
        
        if ("OFFLINE".equals(currentMode)) {
            Toast.makeText(this, "Server not configured. Open Settings first.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        if ("LAN".equals(currentMode)) {
            loadLAN();
        } else if ("REMOTE".equals(currentMode)) {
            loadRemote();
        } else {
            Toast.makeText(this, "Unable to determine server mode", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadLAN() {
        SharedPreferences prefs = getSharedPreferences("ahmyth", MODE_PRIVATE);
        String lanIp = prefs.getString("lan_ip", "");
        String port = prefs.getString("server_port", "42474");
        
        if (lanIp.isEmpty()) {
            loadRemote();
            return;
        }
        
        currentMode = "LAN";
        String url = "http://" + lanIp + ":" + port;
        webView.loadUrl(url);
        showModeIndicator("LAN Mode (Local WiFi - Fast)");
    }

    private void loadRemote() {
        SharedPreferences prefs = getSharedPreferences("ahmyth", MODE_PRIVATE);
        String remoteHost = prefs.getString("server_host", "");
        String port = prefs.getString("server_port", "443");
        
        if (remoteHost.isEmpty()) {
            Toast.makeText(this, "Remote server not configured", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        currentMode = "REMOTE";
        String url = "https://" + remoteHost + ":" + port;
        webView.loadUrl(url);
        showModeIndicator("REMOTE Mode (NGrok)");
    }

    private void showModeIndicator(String mode) {
        // Try to show status bar if exists
        try {
            TextView statusBar = findViewById(R.id.dashboardStatus);
            if (statusBar != null) {
                statusBar.setText("📡 " + mode);
                statusBar.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            // Status bar not available, ignore
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
