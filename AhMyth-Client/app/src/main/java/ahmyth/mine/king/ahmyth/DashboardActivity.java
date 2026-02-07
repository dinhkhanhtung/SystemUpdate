package ahmyth.mine.king.ahmyth;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Toast;

public class DashboardActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
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
        
        // Allow mixed content (HTTP + HTTPS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(DashboardActivity.this, "Error: " + description, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDashboard() {
        SharedPreferences prefs = getSharedPreferences("ahmyth", MODE_PRIVATE);
        String serverHost = prefs.getString("server_host", "");
        String serverPort = prefs.getString("server_port", "42474");
        String lanIp = prefs.getString("lan_ip", "");
        
        // Prefer LAN IP if set
        String urlHost = !lanIp.isEmpty() ? lanIp : serverHost;
        
        if (urlHost.isEmpty()) {
            Toast.makeText(this, "Server not configured. Open Settings first.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Build URL
        String url = "http://" + urlHost + ":" + serverPort;
        
        webView.loadUrl(url);
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
