package de.mhid.opensource.cwadetails.ble;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class BleScanService extends Service {
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

  protected void scanBegin() {
    Log.i("scanResults", "BEGIN");
  }

  protected void scanResult(String mac, int rssi, byte[] data) {
    Log.i("scanResults", "MAC = " + mac + ", RSSI = " + rssi + ", DATA = " + data);
  }

  protected void scanFinished() {
    Log.i("scanResults", "FINISHED");
  }
}
