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
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.mhid.opensource.socialdistancemeter.services.BleScanService;

public class BleScanner extends Thread {
  private static final long SCAN_DURATION = 10000;
  private static final long SCAN_PERIOD_ERROR_RETRY = 10000;

  private static final UUID UUID_ENF = UUID.fromString("0000fd6f-0000-1000-8000-00805f9b34fb");
  private static final UUID[] UUID_FILTER_LIST = new UUID[]{UUID_ENF};
  private List<ScanFilter> scanFilter = null;
  private ScanSettings scanSettings = null;
  private BleScanner.BleScanCallback bleScanCallback = null;

  private BluetoothAdapter bluetoothAdapter = null;
  private BluetoothLeScanner bluetoothLeScanner = null;

  private final BleScanService service;

  private boolean running = false;
  private boolean shutdown = false;
  private long scanPeriod = 50000;
  private long nextSleepTime = 100;

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
    shutdown = false;

    super.start();
  }

  public void shutdown() {
    synchronized (this) {
      if (!running) return;
      if (shutdown) return;

      shutdown = true;
      interrupt();
    }

    while(true) {
      try {
        join();
        break;
      } catch (InterruptedException ignored) {
      }
    }

    synchronized (this) {
      running = false;
    }
  }

  @Override
  public void run() {
    long lastScanTime = System.currentTimeMillis();

    while(true) {
      synchronized (this) {
        // thread aborted?
        if(!running || shutdown) break;
      }

      // wait for next scan period
      long current = System.currentTimeMillis();
      if(current < lastScanTime + getNextSleepTime()) {
        try {
          //noinspection BusyWait
          sleep((lastScanTime + getNextSleepTime()) - current);
        } catch (InterruptedException ignored) {
          continue;
        }
      }

      // check if app has location permission
      boolean hasLocationPermission = ActivityCompat.checkSelfPermission(service, Manifest.permission.ACCESS_FINE_LOCATION) ==
              PackageManager.PERMISSION_GRANTED;
      if(!hasLocationPermission) {
        // -> show error
        service.scanPermissionError();
        Log.i("scheduleScan", "missing permission");
        setNextSleepTime(SCAN_PERIOD_ERROR_RETRY, true);
        continue;
      }

      Log.i("scheduleScan", "running now!");

      long sleepTime;
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        sleepTime = this.scan_Lollipop();
      } else {
        sleepTime = this.scan_Older();
      }
      setNextSleepTime(sleepTime, true);

      Log.i("scheduleScan", "scanning done!");

      lastScanTime = System.currentTimeMillis();
    }
  }


  public synchronized void setPeriod(long p) {
    scanPeriod = p;
    setNextSleepTime(scanPeriod, false);
    interrupt();
  }

  private synchronized long getPeriodWaitTime() {
    return scanPeriod;
  }

  private synchronized void setNextSleepTime(long nextSleepTime, boolean force) {
    if(force || nextSleepTime < this.nextSleepTime) {
      this.nextSleepTime =  nextSleepTime;
    }
  }

  private synchronized long getNextSleepTime() {
    return nextSleepTime;
  }

  private long scan_Older() {
    // TODO implement
    return SCAN_PERIOD_ERROR_RETRY;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private long scan_Lollipop() {
    // get bluetooth adapter
    if(bluetoothAdapter == null) {
      bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    // check for missing or disabled bluetooth adapter
    if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
      // -> show error
      service.scanBluetoothError();
      return SCAN_PERIOD_ERROR_RETRY;
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
    try {
      bluetoothLeScanner.startScan(scanFilter, scanSettings, bleScanCallback);

      long scanUntil = System.currentTimeMillis() + SCAN_DURATION;
      while (true) {
        long current = System.currentTimeMillis();
        if (current >= scanUntil) break;
        synchronized (this) {
          if (shutdown) break;
        }

        try {
          //noinspection BusyWait
          sleep(scanUntil - current);
        } catch (InterruptedException ignored) {
        }
      }

      // stop scan
      bluetoothLeScanner.flushPendingScanResults(bleScanCallback);
      bluetoothLeScanner.stopScan(bleScanCallback);
    } finally {
      service.scanFinished();
    }
    // re-schedule scan
    return getPeriodWaitTime();
  }

}
