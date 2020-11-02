package de.mhid.opensource.cwadetails.ble;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.SortedList;

import java.util.HashMap;
import java.util.List;

public class BleScanService extends Service {
  public static final String INTENT_SCAN_RESULT_COUNT = "scan_result_count";

  private BleScanner bleScanner = null;

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

  private class CwaDevice {
    public int rssi;
    public byte[] data;

    public CwaDevice(int rssi, byte[] data) {
      this.rssi = rssi;
      this.data = data;
    }
  }

  private HashMap<String, CwaDevice> scanResults = new HashMap<>();

  protected void scanBegin() {
    Log.i("scanResults", "BEGIN");
    scanResults.clear();
  }

  protected void scanResult(String mac, int rssi, byte[] data) {
    Log.i("scanResults", "MAC = " + mac + ", RSSI = " + rssi + ", DATA = " + data);
    if(scanResults.containsKey(mac)) return;
    scanResults.put(mac, new CwaDevice(rssi, data));
  }

  protected void scanFinished() {
    Log.i("scanResults", "FINISHED");

    Intent sndUserCount = new Intent();
    sndUserCount.setAction(INTENT_SCAN_RESULT_COUNT);
    sndUserCount.putExtra("count", scanResults.size());
    sendBroadcast(sndUserCount);

    Log.i("scanResults", "Count: " + scanResults.size());
  }
}
