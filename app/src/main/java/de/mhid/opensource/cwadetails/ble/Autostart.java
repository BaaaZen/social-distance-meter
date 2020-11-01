package de.mhid.opensource.cwadetails.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Autostart extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Intent bleServiceIntent = new Intent(context, BleScanService.class);
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//      context.startForegroundService(intent);
//    } else {
      context.startService(bleServiceIntent);
//    }

    Log.i("Autostart", "started");
  }
}
