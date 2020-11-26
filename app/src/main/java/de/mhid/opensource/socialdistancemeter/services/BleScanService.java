package de.mhid.opensource.socialdistancemeter.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.activity.MainActivity;
import de.mhid.opensource.socialdistancemeter.ble.BleScanner;
import de.mhid.opensource.socialdistancemeter.database.CwaToken;
import de.mhid.opensource.socialdistancemeter.database.Database;
import de.mhid.opensource.socialdistancemeter.utils.HexString;

public class BleScanService extends Service {
  private static final String NOTIFICATION_CHANNEL_ID = "ble_scan_notification_channel";

  public static final String INTENT_START_MAIN_ACTIVITY = "request_user_count";

  private BleScanner bleScanner = null;
  private SharedPreferences sharedPreferences = null;

  private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences1, key) -> {
    updateFromPreferences(key);
  };

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    boolean hasLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED;
    boolean hasBackgroundPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED;

    // get current user count
    if(intent.getAction() != null && intent.getAction().equals(INTENT_START_MAIN_ACTIVITY)) {
      if(hasLocationPermission) {
        // send update user count
        // -> do we have a current user count?
        if(recentUserCount != null) {
          // we have a current user count
          // -> send intent
          sendScanResultUserCount(recentUserCount);
        } else {
          sendScanResultUserCount(MainActivity.COUNT_ERROR_SCANNING_IN_PROGRESS);
        }
      } else {
        sendScanResultUserCount(MainActivity.COUNT_ERROR_UNABLE_TO_SCAN);
      }
    }

    // check if we need to request any permissions
    if(!hasLocationPermission || !hasBackgroundPermission) {
      // missing permission
      // -> request permission
      sendIntentRequestPermission();
    }

    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onCreate() {
    super.onCreate();

    bleScanner = new BleScanner(this);

    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

    updateFromPreferences(null);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferences = null;

    bleScanner.shutdown();
    bleScanner = null;
  }

  private void updateFromPreferences(String updatedKey) {
    // scan enabled
    if(updatedKey == null || updatedKey.equals(getString(R.string.settings_key_scan_enabled))) {
      boolean scanEnabled = sharedPreferences.getBoolean(getString(R.string.settings_key_scan_enabled), true);
      if(scanEnabled) {
        bleScanner.start();
      } else {
        bleScanner.shutdown();
        removeNotification();
        return;
      }
    }

    // scan period
    if(updatedKey == null || updatedKey.equals(getString(R.string.settings_key_scan_period))) {
      String scanPeriod = sharedPreferences.getString(getString(R.string.settings_key_scan_period), getString(R.string.settings_list_scan_period_default_value));
      String scanPeriodValues[] = getResources().getStringArray(R.array.settings_list_scan_period_values);
      long scanPeriodValue = 60000;
      if (scanPeriod.equals(scanPeriodValues[0])) {
        scanPeriodValue = 30000; // 30s
      } else if (scanPeriod.equals(scanPeriodValues[1])) {
        scanPeriodValue = 60000; // 1m
      } else if (scanPeriod.equals(scanPeriodValues[2])) {
        scanPeriodValue = 3 * 60000; // 3m
      } else if (scanPeriod.equals(scanPeriodValues[3])) {
        scanPeriodValue = 5 * 60000; // 5m
      }
      bleScanner.setPeriod(scanPeriodValue);
    }

    // scan location enabled
    if(updatedKey == null || updatedKey.equals(getString(R.string.settings_key_scan_location_enabled))) {
      boolean scanLocationEnabled = sharedPreferences.getBoolean(getString(R.string.settings_key_scan_location_enabled), false);
      bleScanner.setScanLocationEnabled(scanLocationEnabled);
    }

    // scan location period
    if(updatedKey == null || updatedKey.equals(getString(R.string.settings_key_scan_location_period))) {
      String scanLocationPeriod = sharedPreferences.getString(getString(R.string.settings_key_scan_location_period), getString(R.string.settings_list_scan_location_period_default_value));
      String scanLocationPeriodValues[] = getResources().getStringArray(R.array.settings_list_scan_location_period_values);
      long scanLocationPeriodValue = 5 * 60000;
      boolean scanLocationPeriodOnGathering = false;
      if (scanLocationPeriod.equals(scanLocationPeriodValues[0])) {
        scanLocationPeriodValue = 0;
      } else if (scanLocationPeriod.equals(scanLocationPeriodValues[1])) {
        scanLocationPeriodValue = 5 * 60000;
      } else if (scanLocationPeriod.equals(scanLocationPeriodValues[2])) {
        scanLocationPeriodValue = 15 * 60000;
      } else if (scanLocationPeriod.equals(scanLocationPeriodValues[3])) {
        scanLocationPeriodValue = 5 * 60000;
        scanLocationPeriodOnGathering = true;
      }
      bleScanner.setScanLocationPeriod(scanLocationPeriodValue, scanLocationPeriodOnGathering);
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private class CwaScanResult {
    private List<Integer> rssiHistory = new ArrayList<>();
    public final byte[] token;
    private Date localTimestamp = new Date();
    int utcOffset = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();

    public CwaScanResult(byte[] token) {
      this.token = token;
    }

    public void addRssi(int rssi) {
      rssiHistory.add(rssi);
    }

    public int getRssi() {
      int rssiItems = 0;
      int rssiSum = 0;
      for(int rssi : rssiHistory) {
        rssiSum += rssi;
        rssiItems++;
      }

      return rssiSum / rssiItems;
    }

    public long getRollingTimestamp() {
      return getUTCTimestamp().getTime() / (10*60*1000);
    }

    public Date getLocalTimestamp() {
      return localTimestamp;
    }
    public Date getUTCTimestamp() { return new Date(getLocalTimestamp().getTime() - utcOffset); }

    public String getHexToken() {
      return HexString.toHexString(token);
    }
  }

  private HashMap<String, CwaScanResult> scanResults = null;
  private Integer recentUserCount = null;

  public void scanBegin() {
    Log.d(getClass().getSimpleName(), "scan begin");
    if(recentUserCount == null) {
      sendScanResultUserCount(MainActivity.COUNT_ERROR_SCANNING_IN_PROGRESS);
    }

    scanResults = new HashMap<>();
  }

  public void scanResult(String mac, int rssi, byte[] data) {
    Log.d(getClass().getSimpleName(), "scanResult: MAC = " + mac + ", RSSI = " + rssi);
    if(scanResults == null) return;
    if(!scanResults.containsKey(mac)) scanResults.put(mac, new CwaScanResult(data));
    CwaScanResult device = scanResults.get(mac);
    device.addRssi(rssi);
  }

  public void scanFinished() {
    int userCount = scanResults.size();

    Log.d(getClass().getSimpleName(), "scan finished, user count = " + userCount);

    // update recent user count
    recentUserCount = userCount;

    // send scan result intent
    sendScanResultUserCount(userCount);

    // write scan results to database
    Database db = Database.getInstance(this);
    db.runAsync(() -> {
      for(String mac : scanResults.keySet()) {
        CwaScanResult scanResult = scanResults.get(mac);

        // generate database object
        CwaToken dbCwaToken = new CwaToken();
        dbCwaToken.mac = mac;
        dbCwaToken.rssi = scanResult.getRssi();
        dbCwaToken.rollingTimestamp = scanResult.getRollingTimestamp();
        dbCwaToken.localTimestamp = scanResult.getLocalTimestamp();
        dbCwaToken.utcTimestamp = scanResult.getUTCTimestamp();
        dbCwaToken.token = scanResult.getHexToken();

        // insert in database
        db.cwaDatabase().cwaToken().insert(dbCwaToken);
      }
    });
  }

  public void scanPermissionError() {
    // no location permission -> no results!
    recentUserCount = null;
    sendScanResultUserCount(MainActivity.COUNT_ERROR_UNABLE_TO_SCAN);
  }

  public void scanBluetoothError() {
    // bluetooth disabled -> no results!
    recentUserCount = null;
    sendScanResultUserCount(MainActivity.COUNT_ERROR_UNABLE_TO_SCAN);
  }

  private void removeNotification() {
    stopForeground(true);
  }

  private void updateNotification(int userCount) {
    // calc text and icon
    String notificationText = MainActivity.getStatusForUserCount(this, userCount);
    int iconRes = MainActivity.getIconForUserCount(this, userCount);

    // generate notification for foreground service
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    NotificationCompat.Builder builder;

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "CWA Details", NotificationManager.IMPORTANCE_MIN);
      notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
      manager.createNotificationChannel(notificationChannel);

      builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
    } else {
      builder = new NotificationCompat.Builder(this);
    }

    builder
            .setContentIntent(pendingIntent)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(notificationText)
            .setSmallIcon(iconRes);

    startForeground(1, builder.build());
  }

  private void sendScanResultUserCount(int userCount) {
    sendIntentScanResult(userCount);
    updateNotification(userCount);
  }

  private void sendIntentScanResult(int userCount) {
    // send intent with user count update
    Intent sndUserCount = new Intent();
    sndUserCount.setAction(MainActivity.INTENT_SCAN_RESULT_COUNT);
    sndUserCount.putExtra(MainActivity.INTENT_SCAN_RESULT_COUNT__COUNT, userCount);
    sendBroadcast(sndUserCount);
  }

  private void sendIntentRequestPermission() {
    // send intent to request permission
    Intent sndRequestPermission = new Intent();
    sndRequestPermission.setAction(MainActivity.INTENT_REQUEST_PERMISSION);
    sendBroadcast(sndRequestPermission);
  }
}
