package mobi.omegacentauri.mutehome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class mutehome extends Activity {

    private SharedPreferences options;
    static final String SETTINGS = "com.android.settings";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.main);

    }

    @Override
    public void onStart() {
        super.onStart();
        CheckBox cb = findViewById(R.id.checkBox);
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(android.Manifest.permission.READ_LOGS)) {
            ((TextView)findViewById(R.id.textView)).setText("For this to work, use your PC to run:\n"+
                    "adb shell grant mobi.omegacentauri.mutehome android.permission.READ_LOGS");
            cb.setEnabled(false);
        }
        else {
            ((TextView)findViewById(R.id.textView)).setText("Ready");
            if (options.getBoolean("active", false)) {
                activate();
                cb.setChecked(true);
            }
            else {
                deactivate();
                cb.setChecked(false);
            }
        }
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    activate();
                else
                    deactivate();
                options.edit().putBoolean("active", b).commit();
            }
        });
    }

    public void onStop() {

        super.onStop();

        CheckBox cb = findViewById(R.id.checkBox);
        cb.setOnCheckedChangeListener(null);
    }

    private void activate() {
        Intent serviceIntent = new Intent(this, Monitoring.class);
        stopService(serviceIntent);
        startForegroundService(serviceIntent);
    }

    private void deactivate() {
        Intent serviceIntent = new Intent(this, Monitoring.class);
        stopService(serviceIntent);
    }
}
