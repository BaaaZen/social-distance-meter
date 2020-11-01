package de.mhid.opensource.cwadetails.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleScanner {
  private static final long SCAN_PERIOD = 10000;
  private static final long SCAN_DURATION = 5000;

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

      service.scanResult(result.getDevice().getAddress(), result.getRssi(), result.getScanRecord().getBytes());
    }
  }

  private Runnable runScheduledScan = () -> {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      this.scan_Lollipop();
    } else {
      this.scan_Older();
    }
  };

  public BleScanner(BleScanService service) {
    this.service = service;

    // initialize environment
    init();

    // schedule scan
    scheduleScan();
  }

  private void init() {
    if(bluetoothAdapter == null) {
      final BluetoothManager bluetoothManager = (BluetoothManager)service.getSystemService(Context.BLUETOOTH_SERVICE);
      bluetoothAdapter = bluetoothManager.getAdapter();
    }
  }

  protected void shutdown() {
    shutdown = true;
    synchronized (scanRunning) {
      if(scanRunning) return;
    }

    // seems like we are waiting for scheduled scan -> abort schedule
    handler.removeCallbacks(runScheduledScan);
  }

  private void scheduleScan() {
    if(shutdown) return;
    handler.postDelayed(runScheduledScan, SCAN_PERIOD);
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
      bluetoothLeScanner.flushPendingScanResults(bleScanCallback);
      bluetoothLeScanner.stopScan(bleScanCallback);
      service.scanFinished();
      synchronized (scanRunning) {
        scanRunning = false;
      }
      // re-schedule scan
      scheduleScan();
    }, SCAN_DURATION);
  }

}
