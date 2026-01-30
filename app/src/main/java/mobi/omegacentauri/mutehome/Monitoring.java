package mobi.omegacentauri.mutehome;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobi.omegacentauri.mutehome.mutehome;

public class Monitoring extends Service {
    private static final String CHANNEL_ID = "AppMonitorChannel";
    private boolean isMonitoring = false;
    private String lastPackageName = "";
    private NotificationChannel mChannel;
    private SharedPreferences options;
    private boolean muted = false;
    private int savedVolume = 10;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        options = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void setMedia(boolean state) {
        int volumeLevel;

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (audioManager != null) {
            if (!state) {
                if (!muted) {
                    savedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    Log.v("MuteHome", "Saving "+savedVolume);
                }
                muted = true;
                volumeLevel = 0;
            }
            else {
                muted = false;
                if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)==0) {
                    Log.v("MuteHome", "Restoring "+savedVolume);
                    volumeLevel = savedVolume;
                }
                else
                    return;
            }



            // volumeLevel is an integer.
            // 0 is mute, and the max depends on the device (usually 15).

            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC, // The Media stream
                    volumeLevel,               // The volume index
                    0                          // Flags (e.g., AudioManager.FLAG_SHOW_UI)
            );
        }
    }

    @SuppressLint({"ForegroundServiceType", "WrongConstant"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String channelId = "barometer_channel";
        Notification.Builder nb;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nb = new Notification.Builder(this, channelId);
            mChannel = new NotificationChannel(channelId, "Mute Home", NotificationManager.IMPORTANCE_LOW);
            // Configure the notification channel.
            mChannel.setDescription("Mute Home monitoring");
            mChannel.enableLights(false);
            mChannel.setVibrationPattern(null);
            mNotificationManager.createNotificationChannel(mChannel);
            nb.setChannelId(channelId);
        }
        else {
            nb = new Notification.Builder(this);
        }
        nb.setOngoing(true);
        Intent activityIntent = new Intent(this, mutehome.class);
        nb.setContentIntent(PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE));
        nb.setContentText("Collecting barometer/altitude data");
  //      nb.setSmallIcon(R.drawable.updown); // todo
        nb.setContentTitle("Giant Barometer");
        Notification notification = nb.build();
        if (notification == null) {
            Log.e("mutehome", "null notification");
            // don't know what to do or how it can happen
        }
        if (Build.VERSION.SDK_INT >= 29) {
            Log.v("GiantBarometer", "service");
            startForeground(startId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);//0x40000000
        }
        else {
            startForeground(startId, notification);
        }

        if (!isMonitoring) {
            isMonitoring = true;
            startLogcatThread();
        }

        return START_STICKY;
    }

    private void startLogcatThread() {
        new Thread(() -> {
            try {
                // Pre-compile regex for performance
                // Matches "Displayed com.package.name"
                Pattern pattern = Pattern.compile("wm_on_(start|stop)_called: .*com\\.oculus\\.vrshell\\.HomeActivity");

                // Clear logcat buffer first to avoid old logs triggering events
                Runtime.getRuntime().exec("logcat -c");

                // Read the ActivityTaskManager logs
                Process process = Runtime.getRuntime().exec("logcat -b events");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                Log.v("MuteHome", "monitoring "+isMonitoring);
                while (isMonitoring && (line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String packageName = matcher.group(1);
                        handleHomeDetected(packageName.equals("stop"));
                    }
                }
                Log.v("MuteHome", "monitoring done");
            } catch (Exception e) {
                Log.e("LogcatService", "Error reading logcat", e);
            }
        }).start();
    }

    private void handleHomeDetected(boolean stop) {
        Log.v("MuteHome", "value "+stop);
        setMedia(stop);
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "App Monitor Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        isMonitoring = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}