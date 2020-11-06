package de.mhid.opensource.cwadetails.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleScanner {
  private static final long SCAN_PERIOD_ERROR_RETRY = 10000;
  private static final long SCAN_PERIOD_NORMAL = 30000;
  private static final long SCAN_DURATION = 10000;

  private static final UUID UUID_ENF = UUID.fromString("0000fd6f-0000-1000-8000-00805f9b34fb");
  private static final UUID[] UUID_FILTER_LIST = new UUID[]{UUID_ENF};
  private List<ScanFilter> scanFilter = null;
  private ScanSettings scanSettings = null;
  private BleScanner.BleScanCallback bleScanCallback = null;

  private BluetoothAdapter bluetoothAdapter = null;
  private BluetoothLeScanner bluetoothLeScanner = null;

  private BleScanService service;

  private Handler handler = new Handler(Looper.myLooper());
  private Boolean scanRunning = false;
  private Boolean shutdown = false;

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  class BleScanCallback extends ScanCallback {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      super.onScanResult(callbackType, result);

      service.scanResult(result.getDevice().getAddress(), result.getRssi(), result.getScanRecord().getServiceData(new ParcelUuid(UUID_ENF)));
    }
  }

  public BleScanner(BleScanService service) {
    this.service = service;

    // schedule scan
    scheduleScan(100);
  }

  protected void shutdown() {
    shutdown = true;
    synchronized (scanRunning) {
      if(scanRunning) return;
    }

    // seems like we are waiting for scheduled scan -> abort schedule
    handler.removeCallbacks(runScheduledScan);
  }

  private Runnable runScheduledScan = () -> {
    Log.i("scheduleScan", "running now!");
    // check if app has location permission
    boolean hasLocationPermission = ActivityCompat.checkSelfPermission(service, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED;
    if(!hasLocationPermission) {
      // -> show error
      service.scanPermissionError();
      scheduleScan(SCAN_PERIOD_ERROR_RETRY);
      return;
    }

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      this.scan_Lollipop();
    } else {
      this.scan_Older();
    }
  };

  private void scheduleScan(long waitDuration) {
    if(shutdown) return;
    Log.i("scheduleScan", "scheduling after " + waitDuration/1000 + "s");
    handler.postDelayed(runScheduledScan, waitDuration);
  }

  private void scan_Older() {
    // TODO
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void scan_Lollipop() {
    synchronized (scanRunning) {
      if(scanRunning) return;
      scanRunning = true;
    }

    // get bluetooth adapter
    if(bluetoothAdapter == null) {
      bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    // check for missing or disabled bluetooth adapter
    if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
      // -> show error
      service.scanBluetoothError();
      synchronized (scanRunning) {
        scanRunning = false;
      }
      scheduleScan(SCAN_PERIOD_ERROR_RETRY);
      return;
    }

    // init bluetooth le scanner
    if(bluetoothLeScanner == null) {
      bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    // init scan filter
    if(scanFilter == null) {
      scanFilter = new ArrayList<>();
      for (UUID uuid : UUID_FILTER_LIST) {
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(uuid))
                .build();
        scanFilter.add(filter);
      }
    }

    // init scan settings
    if(scanSettings == null) {
      scanSettings = new ScanSettings.Builder()
              .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
              .setReportDelay(0L)
              .build();
    }

    // init scan callback
    if(bleScanCallback == null) {
      bleScanCallback = new BleScanner.BleScanCallback();
    }

    // start scan
    service.scanBegin();
    bluetoothLeScanner.startScan(scanFilter, scanSettings, bleScanCallback);

    // stop scan after x seconds
    new Handler(Looper.myLooper()).postDelayed(() -> {
      try {
        bluetoothLeScanner.flushPendingScanResults(bleScanCallback);
        bluetoothLeScanner.stopScan(bleScanCallback);
        service.scanFinished();
      } finally {
        synchronized (scanRunning) {
          scanRunning = false;
        }

        // re-schedule scan
        scheduleScan(SCAN_PERIOD_NORMAL);
      }
    }, SCAN_DURATION);
  }

}
