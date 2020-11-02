package de.mhid.opensource.cwadetails.ble;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.mhid.opensource.cwadetails.activity.MainActivity;
import de.mhid.opensource.cwadetails.database.CwaToken;
import de.mhid.opensource.cwadetails.database.Database;

public class BleScanService extends Service {
  public static final String INTENT_REQUEST_USER_COUNT = "request_user_count";

  private BleScanner bleScanner = null;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(intent.getAction().equals(INTENT_REQUEST_USER_COUNT)) {
      // request for user count
      // -> do we have a current user count?
      if(recentUserCount != null) {
        // we have a current user count
        // -> send intent
        sendIntentScanResult(recentUserCount);
      }
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

  private HashMap<String, CwaScanResult> scanResults = new HashMap<>();
  private Date scanTimestamp = null;
  private Integer recentUserCount = null;

  protected void scanBegin() {
    Log.d("scanResults", "BEGIN");
    scanResults.clear();
    scanTimestamp = new Date();
  }

  protected void scanResult(String mac, int rssi, byte[] data) {
    Log.d("scanResults", "MAC = " + mac + ", RSSI = " + rssi + ", DATA = " + data);
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

  private void sendIntentScanResult(int userCount) {
    // send intent with user count update
    Intent sndUserCount = new Intent();
    sndUserCount.setAction(MainActivity.INTENT_SCAN_RESULT_COUNT);
    sndUserCount.putExtra(MainActivity.INTENT_SCAN_RESULT_COUNT__COUNT, userCount);
    sendBroadcast(sndUserCount);
  }

  protected void scanFinished() {
    Log.d("scanResults", "FINISHED");

    int userCount = scanResults.size();

    // update recent user count
    recentUserCount = userCount;

    // send scan result intent
    sendIntentScanResult(userCount);

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
}
