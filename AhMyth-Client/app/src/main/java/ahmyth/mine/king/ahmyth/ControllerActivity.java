package ahmyth.mine.king.ahmyth;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

/**
 * ControllerActivity - Điều Khiển Từ Điện Thoại
 * 
 * Cho phép bạn từ điện thoại của mình:
 * 1. Kết nối tới AhMyth-Server
 * 2. Xem danh sách devices
 * 3. Chọn device và điều khiển
 */
public class ControllerActivity extends Activity {
    private WebView webView;
    private EditText etServerUrl;
    private Button btnConnect;
    private SharedPreferences prefs;
    private String currentServerUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        
        prefs = getSharedPreferences("ahmyth_controller", MODE_PRIVATE);
        
        // Setup WebView
        webView = findViewById(R.id.webView);
        etServerUrl = findViewById(R.id.etServerUrl);
        btnConnect = findViewById(R.id.btnConnect);
        
        // Load saved server URL
        String savedUrl = prefs.getString("server_url", "http://192.168.1.x:42474");
        etServerUrl.setText(savedUrl);
        
        // Configure WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // Enable mixed content (HTTP + HTTPS)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.webkit.WebView.setWebContentsDebuggingEnabled(true);
            settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(android.webkit.WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                btnConnect.setText("Loading...");
                btnConnect.setEnabled(false);
            }
            
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                btnConnect.setText("Reload");
                btnConnect.setEnabled(true);
            }
            
            @Override
            public void onReceivedError(android.webkit.WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                Toast.makeText(ControllerActivity.this, "Connection failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Connect button
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToServer();
            }
        });
        
        // Try to load saved URL
        String savedServerUrl = prefs.getString("server_url", null);
        if (savedServerUrl != null && !savedServerUrl.isEmpty()) {
            loadDashboard(savedServerUrl);
        }
    }
    
    private void connectToServer() {
        String url = etServerUrl.getText().toString().trim();
        
        if (url.isEmpty()) {
            Toast.makeText(this, "Nhập URL server", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save URL
        prefs.edit().putString("server_url", url).apply();
        
        // Ensure URL has protocol
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        
        currentServerUrl = url;
        loadDashboard(url);
    }
    
    private void loadDashboard(String serverUrl) {
        try {
            // Try to load dashboard from server
            if (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }
            
            String dashboardUrl = serverUrl + "/";
            webView.loadUrl(dashboardUrl);
            
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
