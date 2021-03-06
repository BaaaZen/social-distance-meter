/*
Social Distance Meter - An app to analyze and rate your social distancing behavior
Copyright (C) 2020-2021  Mirko Hansen (baaazen@gmail.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package de.mhid.opensource.socialdistancemeter.services;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.activity.MainActivity;
import de.mhid.opensource.socialdistancemeter.ble.BleScanner;
import de.mhid.opensource.socialdistancemeter.database.CwaToken;
import de.mhid.opensource.socialdistancemeter.database.Database;
import de.mhid.opensource.socialdistancemeter.notification.NotificationChannelHelper;
import de.mhid.opensource.socialdistancemeter.services.work.PurgeTokenFromDatabaseWorker;
import de.mhid.opensource.socialdistancemeter.utils.HexString;
import de.mhid.opensource.socialdistancemeter.utils.Timestamp;

public class BleScanService extends Service {
  public final static String INTENT_REQUEST_USER_COUNT = "request_user_count";

  private final static String PERIODIC_PRUNE_WORK_NAME = "periodic_prune_token_from_database_work";

  private static HashMap<String, CwaScanResult> staticScanResults = null;

  private static synchronized void setScanResults(HashMap<String, CwaScanResult> scanResults) {
    if(scanResults == null) scanResults = new HashMap<>();
    BleScanService.staticScanResults = scanResults;
  }

  public static synchronized HashMap<String, CwaScanResult> getScanResults() {
    return staticScanResults;
  }

  private BleScanner bleScanner = null;
  private SharedPreferences sharedPreferences = null;
  private LocationManager locationManager = null;

  private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences1, key) -> updateFromPreferences(key);

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    boolean hasLocationPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED;
    boolean hasBackgroundPermission = true;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
      hasBackgroundPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
              PackageManager.PERMISSION_GRANTED;
    }

    // get current user count
    if (intent.getAction() != null && intent.getAction().equals(INTENT_REQUEST_USER_COUNT)) {
      if (hasLocationPermission) {
        // send update user count
        // -> do we have a current user count?
        if (recentUserCount != null) {
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
    if (!hasLocationPermission || !hasBackgroundPermission) {
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

    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    createPruneWork();

    updateFromPreferences(null);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    destroyPruneWork();

    locationManager = null;

    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferences = null;

    bleScanner.shutdown();
    bleScanner = null;
  }

  private void createPruneWork() {
    Constraints constraints = new Constraints.Builder()
      .setRequiresBatteryNotLow(true)
      .build();

    PeriodicWorkRequest workRequest =
            new PeriodicWorkRequest.Builder(PurgeTokenFromDatabaseWorker.class, 1, TimeUnit.DAYS, 12, TimeUnit.HOURS)
                  .setConstraints(constraints)
                  .setInitialDelay(10, TimeUnit.MINUTES)
                  .build();

    WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(PERIODIC_PRUNE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, workRequest);
  }

  private void destroyPruneWork() {
    WorkManager.getInstance(this)
            .cancelUniqueWork(PERIODIC_PRUNE_WORK_NAME);
  }

  private void updateFromPreferences(String updatedKey) {
    // scan enabled
    if (updatedKey == null || updatedKey.equals(getString(R.string.settings_key_scan_enabled))) {
      boolean scanEnabled = sharedPreferences.getBoolean(getString(R.string.settings_key_scan_enabled), true);
      if (scanEnabled) {
        bleScanner.start();
      } else {
        bleScanner.shutdown();
        removeNotification();
        return;
      }
    }

    // scan period
    if (updatedKey == null || updatedKey.equals(getString(R.string.settings_key_scan_period))) {
      String scanPeriod = sharedPreferences.getString(getString(R.string.settings_key_scan_period), getString(R.string.settings_list_scan_period_default_value));
      String[] scanPeriodValues = getResources().getStringArray(R.array.settings_list_scan_period_values);
      long scanPeriodValue = 60000;
      if (scanPeriod.equals(scanPeriodValues[0])) {
        scanPeriodValue = 60000; // 1m
      } else if (scanPeriod.equals(scanPeriodValues[1])) {
        scanPeriodValue = 2 * 60000; // 2m
      } else if (scanPeriod.equals(scanPeriodValues[2])) {
        scanPeriodValue = 3 * 60000; // 3m
      } else if (scanPeriod.equals(scanPeriodValues[3])) {
        scanPeriodValue = 5 * 60000; // 5m
      }
      bleScanner.setPeriod(scanPeriodValue);
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  public static class CwaScanResult implements Comparable<CwaScanResult> {
    private final List<Integer> rssiHistory = new ArrayList<>();
    private final byte[] token;
    private final Timestamp timestamp = Timestamp.getCurrentTimestamp();

    public CwaScanResult(byte[] token) {
      this.token = token;
    }

    @Override
    public int compareTo(CwaScanResult o) {
      return getRssi() - o.getRssi();
    }

    public void addRssi(int rssi) {
      rssiHistory.add(rssi);
    }

    public int getRssi() {
      int rssiItems = 0;
      int rssiSum = 0;
      for (int rssi : rssiHistory) {
        rssiSum += rssi;
        rssiItems++;
      }

      return rssiSum / rssiItems;
    }

    public Timestamp getTimestamp() {
      return timestamp;
    }

    public String getHexToken() {
      return HexString.toHexString(token);
    }
  }

  private HashMap<String, CwaScanResult> scanResults = null;
  private Integer recentUserCount = null;

  public void scanBegin() {
    Log.d(getClass().getSimpleName(), "scan begin");
    if (recentUserCount == null) {
      sendScanResultUserCount(MainActivity.COUNT_ERROR_SCANNING_IN_PROGRESS);
    }

    scanResults = new HashMap<>();
  }

  public void scanResult(String mac, int rssi, byte[] data) {
    Log.d(getClass().getSimpleName(), "scanResult: MAC = " + mac + ", RSSI = " + rssi);
    if (scanResults == null) return;
    if (!scanResults.containsKey(mac)) scanResults.put(mac, new CwaScanResult(data));
    CwaScanResult device = scanResults.get(mac);
    assert device != null;
    device.addRssi(rssi);
  }

  public void scanFinished() {
    int userCount = scanResults.size();

    Log.d(getClass().getSimpleName(), "scan finished, user count = " + userCount);

    // store current scan results
    setScanResults(scanResults);

    // update recent user count
    recentUserCount = userCount;

    // send scan result intent
    sendScanResultUserCount(userCount);

    // get current location
    Location location = getLocation(userCount >= 5);

    // write scan results to database
    Database db = Database.getInstance(this);
    db.runAsync(() -> {
      for (String mac : scanResults.keySet()) {
        CwaScanResult scanResult = scanResults.get(mac);
        assert scanResult != null;

        // generate database object
        CwaToken dbCwaToken = new CwaToken();
        dbCwaToken.mac = mac;
        dbCwaToken.rssi = scanResult.getRssi();
        dbCwaToken.rollingTimestamp = scanResult.getTimestamp().getRollingTimestamp();
        dbCwaToken.localTimestamp = scanResult.getTimestamp().getLocalTimestamp();
        dbCwaToken.utcTimestamp = scanResult.getTimestamp().getUTCTimestamp();
        dbCwaToken.token = scanResult.getHexToken();

        if(location != null) {
          dbCwaToken.longitude = location.getLongitude();
          dbCwaToken.latitude = location.getLatitude();
        }

        // insert in database
        db.cwaDatabase().cwaToken().insert(dbCwaToken);
      }
    });
  }

  private Location getLocation(boolean gathering) {
    boolean locationActive = sharedPreferences.getBoolean(getString(R.string.settings_key_scan_location_enabled), false);

    // scanning location disabled?
    if (!locationActive) return null;

    // do we have permission to get location?
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      return null;
    }

    boolean locationGPS = sharedPreferences.getBoolean(getString(R.string.settings_key_scan_location_use_gps), false);
    if(locationGPS && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) locationGPS = false;

    // first try to get a recent location via passive provide
    Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
    if(location == null || (locationGPS && !location.getProvider().equals(LocationManager.GPS_PROVIDER))) {
      // no location from passive service available or not accurate enough
      location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    String scanLocationPeriod = sharedPreferences.getString(getString(R.string.settings_key_scan_location_period), getString(R.string.settings_list_scan_location_period_default_value));
    String[] scanLocationPeriodValues = getResources().getStringArray(R.array.settings_list_scan_location_period_values);
    int scanLocationMinPeriodValue = 5;
    int scanLocationMaxPeriodValue = 5;
    boolean scanLocationPeriodOnGathering = false;
    if (scanLocationPeriod.equals(scanLocationPeriodValues[0])) {
      scanLocationMinPeriodValue = 1;
      scanLocationMaxPeriodValue = 5;
    } else if (scanLocationPeriod.equals(scanLocationPeriodValues[1])) {
      scanLocationMinPeriodValue = 5;
      scanLocationMaxPeriodValue = 5;
    } else if (scanLocationPeriod.equals(scanLocationPeriodValues[2])) {
      scanLocationMinPeriodValue = 15;
      scanLocationMaxPeriodValue = 15;
    } else if (scanLocationPeriod.equals(scanLocationPeriodValues[3])) {
      scanLocationMinPeriodValue = 5;
      scanLocationMaxPeriodValue = 15;
      scanLocationPeriodOnGathering = true;
    }

    if(location != null && location.getTime() + scanLocationMinPeriodValue*60*1000 > new Date().getTime()) {
      // location is very current - perfect!
      // -> less than scanLocationMinPeriodValue minutes old
      return location;
    }

    if(location == null ||
            (scanLocationPeriodOnGathering && gathering) ||
            location.getTime() + scanLocationMaxPeriodValue*60*1000 < new Date().getTime()) {
      // * no location
      // * gathering detected (where trigger is enabled)
      // * location is older than scanLocationMaxPeriodValue minutes
      // -> update location
      if(locationGPS) {
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, location1 -> { }, Looper.getMainLooper());
      } else {
        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, location1 -> { }, Looper.getMainLooper());
      }
    }

    return location;
  }

  public void scanPermissionError() {
    // no location permission -> no results!
    recentUserCount = null;
    setScanResults(null);
    sendScanResultUserCount(MainActivity.COUNT_ERROR_UNABLE_TO_SCAN);
  }

  public void scanBluetoothError() {
    // bluetooth disabled -> no results!
    recentUserCount = null;
    setScanResults(null);
    sendScanResultUserCount(MainActivity.COUNT_ERROR_UNABLE_TO_SCAN);
  }

  private void removeNotification() {
    stopForeground(true);
  }

  private void updateNotification(int userCount) {
    // calc text and icon
    String notificationText = MainActivity.getStatusForUserCount(this, userCount);
    int iconRes = MainActivity.getIconForUserCount(userCount);

    // generate notification for foreground service
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    // build notification
    NotificationCompat.Builder builder =
            new NotificationCompat.Builder(this, NotificationChannelHelper.getChannelId(this))
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
