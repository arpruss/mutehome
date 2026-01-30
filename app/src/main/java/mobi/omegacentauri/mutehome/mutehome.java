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

public class mutehome extends Activity {

    private SharedPreferences options;
    static final String SETTINGS = "com.android.settings";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options = PreferenceManager.getDefaultSharedPreferences(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(android.Manifest.permission.READ_LOGS)) {
            needPermission();
        }
        else {
            Intent serviceIntent = new Intent(this, Monitoring.class);
            stopService(serviceIntent);
            startForegroundService(serviceIntent);
            havePermission();
        }
    }

    private void needPermission() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Mute Home");
        b.setMessage("For this to work, you will need to run adb shell grant mobi.omegacentauri.mutehome android.permission.READ_LOGS.");
        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        b.create().show();
    }

    private void havePermission() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Mute Home");
        b.setMessage("Mute Home has been activated.");
        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        b.create().show();
    }

    private void goToSettings() {
        PackageManager pm = getPackageManager();
        try {
            Intent i = pm.getLaunchIntentForPackage(SETTINGS);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(i);
        }
        catch(Exception e) {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + SETTINGS));
            i.setPackage(SETTINGS);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(i);
        }
        finish();
    }

}
