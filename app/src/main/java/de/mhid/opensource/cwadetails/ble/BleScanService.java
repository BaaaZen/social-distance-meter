package de.mhid.opensource.cwadetails.ble;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.mhid.opensource.cwadetails.R;
import de.mhid.opensource.cwadetails.activity.MainActivity;
import de.mhid.opensource.cwadetails.database.CwaToken;
import de.mhid.opensource.cwadetails.database.Database;

public class BleScanService extends Service {
  private static final String NOTIFICATION_CHANNEL_ID = "ble_scan_notification_channel";

  public static final String INTENT_START_MAIN_ACTIVITY = "request_user_count";

  private BleScanner bleScanner = null;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    updateNotification(recentUserCount);

    boolean hasLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED;
    boolean hasBackgroundPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED;

    // get current user count
    if(intent.getAction().equals(INTENT_START_MAIN_ACTIVITY)) {
      if(hasLocationPermission) {
        // send update user count
        // -> do we have a current user count?
        if(recentUserCount != null) {
          // we have a current user count
          // -> send intent
          sendScanResultUserCount(recentUserCount);
        }
      } else {
        sendScanResultUserCount(-1);
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
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    bleScanner.shutdown();
    bleScanner = null;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private final static char[] HEX_CONVERT_ARRAY = "0123456789ABCDEF".toCharArray();
  private class CwaScanResult {
    private List<Integer> rssiHistory = new ArrayList<>();
    public final byte[] token;

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

    public String getHexToken() {
      char[] hexChars = new char[token.length * 2];
      for (int j = 0; j < token.length; j++) {
        int v = token[j] & 0xFF;
        hexChars[j * 2] = HEX_CONVERT_ARRAY[v >>> 4];
        hexChars[j * 2 + 1] = HEX_CONVERT_ARRAY[v & 0x0F];
      }
      return new String(hexChars);
    }
  }

  private HashMap<String, CwaScanResult> scanResults = null;
  private Date scanTimestamp = null;
  private Integer recentUserCount = null;

  protected void scanBegin() {
    Log.d("scanResults", "BEGIN");
    scanResults = new HashMap<>();
    scanTimestamp = new Date();
  }

  protected void scanResult(String mac, int rssi, byte[] data) {
    Log.d("scanResults", "MAC = " + mac + ", RSSI = " + rssi + ", DATA = " + data);
    if(scanResults == null) return;
    if(!scanResults.containsKey(mac)) scanResults.put(mac, new CwaScanResult(data));
    CwaScanResult device;
    if(scanResults.containsKey(mac)) {
      device = scanResults.get(mac);
    } else {
      device = new CwaScanResult(data);
      scanResults.put(mac, device);
    }
    device.addRssi(rssi);
  }

  protected void scanFinished() {
    Log.d("scanResults", "FINISHED");

    boolean hasLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED;
    if(!hasLocationPermission) {
      // no location permission -> no results!
      recentUserCount = null;
      sendScanResultUserCount(-1);
      return;
    }

    int userCount = scanResults.size();

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
        dbCwaToken.timestamp = scanTimestamp;
        dbCwaToken.token = scanResult.getHexToken();

        // insert in database
        db.cwaDatabase().cwaTokenDao().insert(dbCwaToken);
      }
    });

    Log.d("scanResults", "Count: " + scanResults.size());
  }

  private void updateNotification(Integer userCount) {
    if(userCount == null) userCount = 0;

    // calc text and icon
    String nText;
    if(userCount >= 0) {
      // valid user count -> update output
      nText = getResources().getQuantityString(R.plurals.card_current_users_count, userCount, userCount);
    } else {
      // invalid user count -> maybe error?
      nText = getResources().getString(R.string.card_current_users_unknown);
    }
    int iconRes;
    if(userCount <= 0) {
      iconRes = R.drawable.round_person_outline_24;
    } else if(userCount == 1) {
      iconRes = R.drawable.round_person_24;
    } else if(userCount == 2) {
      iconRes = R.drawable.round_people_24;
    } else {
      iconRes = R.drawable.round_groups_24;
    }

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
            .setContentText(nText)
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
