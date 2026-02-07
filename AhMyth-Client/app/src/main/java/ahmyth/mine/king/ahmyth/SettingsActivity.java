package ahmyth.mine.king.ahmyth;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final EditText hostView = findViewById(R.id.editHost);
        final EditText portView = findViewById(R.id.editPort);
        final Button btnSave = findViewById(R.id.btnSave);

        final SharedPreferences prefs = getSharedPreferences("ahmyth_prefs", Context.MODE_PRIVATE);

        String defaultHost = getString(R.string.server_ip);
        String defaultPort = getString(R.string.server_port);

        hostView.setText(prefs.getString("server_host", defaultHost));
        portView.setText(prefs.getString("server_port", defaultPort));

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String host = hostView.getText().toString().trim();
                String port = portView.getText().toString().trim();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("server_host", host);
                editor.putString("server_port", port);
                editor.apply();
                Toast.makeText(SettingsActivity.this, "Saved server settings", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
