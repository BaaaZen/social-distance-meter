package de.mhid.opensource.cwadetails.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Autostart extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    // start ble scanner
    Intent bleServiceIntent = new Intent(context, BleScanService.class);
    context.startService(bleServiceIntent);

    // start diag key updates
    Intent diagKeyUpdateServiceIntent = new Intent(context, DiagKeyUpdateService.class);
    context.startService(diagKeyUpdateServiceIntent);

    Log.i(getClass().getSimpleName(), "Autostart started");
  }
}
