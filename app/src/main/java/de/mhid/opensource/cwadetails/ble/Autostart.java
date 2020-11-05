package de.mhid.opensource.cwadetails.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Autostart extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Intent bleServiceIntent = new Intent(context, BleScanService.class);
    context.startService(bleServiceIntent);

    Log.i("Autostart", "started");
  }
}
