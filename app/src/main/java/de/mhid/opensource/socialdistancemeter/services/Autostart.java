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
package de.mhid.opensource.socialdistancemeter.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class Autostart extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
      // start ble scanner
      Intent bleServiceIntent = new Intent(context, BleScanService.class);
      bleServiceIntent.setAction(BleScanService.INTENT_REQUEST_USER_COUNT);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(bleServiceIntent);
      } else {
        context.startService(bleServiceIntent);
      }

      // start diag key updates
      Intent diagKeyUpdateServiceIntent = new Intent(context, DiagKeySyncService.class);
      context.startService(diagKeyUpdateServiceIntent);

      Log.i(getClass().getSimpleName(), "Autostart started");
    }
  }
}
