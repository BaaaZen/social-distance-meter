/*
Social Distance Meter - An app to analyze and rate your social distancing behavior
Copyright (C) 2020  Mirko Hansen (baaazen@gmail.com)

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
package de.mhid.opensource.socialdistancemeter.ble;

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

import de.mhid.opensource.socialdistancemeter.services.BleScanService;

public class BleScanner {
  private static final long SCAN_DURATION = 10000;
  private static final long SCAN_PERIOD_ERROR_RETRY = 10000;

  private static final UUID UUID_ENF = UUID.fromString("0000fd6f-0000-1000-8000-00805f9b34fb");
  private static final UUID[] UUID_FILTER_LIST = new UUID[]{UUID_ENF};
  private List<ScanFilter> scanFilter = null;
  private ScanSettings scanSettings = null;
  private BleScanner.BleScanCallback bleScanCallback = null;

  private BluetoothAdapter bluetoothAdapter = null;
  private BluetoothLeScanner bluetoothLeScanner = null;

  private BleScanService service;

  private final Handler handler = new Handler(Looper.myLooper());
  private boolean scanning = false;
  private boolean running = false;
  private long scanPeriod = 50000;

  private final Runnable runScheduledScan = () -> {
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
  }

  public synchronized void start() {
    if(running) return;

    // schedule scan
    running = true;
    scheduleScan(100);
  }

  public synchronized void shutdown() {
    if(!running) return;

    // seems like we are waiting for scheduled scan -> abort schedule
    running = false;
    handler.removeCallbacks(runScheduledScan);
  }

  private synchronized boolean startScanning() {
    if(scanning) return false;
    scanning = true;
    return true;
  }

  private synchronized boolean stopScanning() {
    if(!scanning) return false;
    scanning = false;
    return true;
  }

  private synchronized void scheduleScan(long waitDuration) {
    if(!running) return;

    Log.i("scheduleScan", "scheduling after " + waitDuration/1000 + "s");
    boolean ok = handler.postDelayed(runScheduledScan, waitDuration);
    if(!ok) {
      Log.e(getClass().getSimpleName(), "Error scheduling scan!");
      running = false;
    }
  }

  public synchronized void setPeriod(long p) {
    scanPeriod = p - SCAN_DURATION;
  }

  private synchronized long getPeriodWaitTime() {
    return scanPeriod;
  }

  private void scan_Older() {
    // TODO
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void scan_Lollipop() {
    boolean ok1 = startScanning();
    if(!ok1) return;

    // get bluetooth adapter
    if(bluetoothAdapter == null) {
      bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    // check for missing or disabled bluetooth adapter
    if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
      // -> show error
      service.scanBluetoothError();
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

    final Runnable afterScan = () -> {
      try {
        bluetoothLeScanner.flushPendingScanResults(bleScanCallback);
        bluetoothLeScanner.stopScan(bleScanCallback);
      } finally {
        boolean ok3 = stopScanning();

        if(ok3) {
          service.scanFinished();

          // re-schedule scan
          scheduleScan(getPeriodWaitTime());
        }
      }
    };

    // stop scan after x seconds
    boolean ok2 = handler.postDelayed(afterScan, SCAN_DURATION);
    if(!ok2) {
      Log.e(getClass().getSimpleName(), "Error scheduling scan stop.");
      afterScan.run();
    }
  }

}
