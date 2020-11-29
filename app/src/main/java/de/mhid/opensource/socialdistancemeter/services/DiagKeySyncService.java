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

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.activity.maincards.CardRisks;
import de.mhid.opensource.socialdistancemeter.database.CwaDiagKeyCount;
import de.mhid.opensource.socialdistancemeter.database.Database;
import de.mhid.opensource.socialdistancemeter.diagkeys.DiagKeySyncWorker;

public class DiagKeySyncService extends Service {
    private final static String PERIODIC_WORK_NAME = "periodic_diag_key_update_work";
    private final static String ONE_TIME_WORK_NAME = "one_time_diag_key_update_work";

    public final static String INTENT_START_DIAG_KEY_UPDATE = "start_diag_key_update";
    public final static String INTENT_GET_UPDATES = "get_updates";

    private SharedPreferences sharedPreferences = null;

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        initPeriodicWork();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction() != null) {
            if(intent.getAction().equals(INTENT_START_DIAG_KEY_UPDATE)) {
                // start update/sync
                startOneTimeWork();
            } else if(intent.getAction().equals(INTENT_GET_UPDATES)) {
                // get last update timestamp + diag key count + sync state
                getUpdates();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        WorkManager.getInstance(this)
                .cancelUniqueWork(PERIODIC_WORK_NAME);

        WorkManager.getInstance(this)
                .cancelUniqueWork(ONE_TIME_WORK_NAME);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void initPeriodicWork() {
        Constraints.Builder constraintBuilder = new Constraints.Builder()
                .setRequiresBatteryNotLow(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            constraintBuilder.setRequiresDeviceIdle(true);
        }
        Constraints constraints = constraintBuilder.build();

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(DiagKeySyncWorker.class, 3, TimeUnit.HOURS, 2, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .setInitialDelay(5, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }

    private void startOneTimeWork() {
        Data.Builder data = new Data.Builder();
        data.putBoolean(DiagKeySyncWorker.WORK_PARAMETER_BACKGROUND, false);

        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(DiagKeySyncWorker.class)
                        .setInputData(data.build())
                        .build();

        WorkManager.getInstance(this)
                .enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, workRequest);
    }

    private void sendIntentLastUpdate(String date) {
        // send intent with last update timestamp
        Intent sndLastUpdate = new Intent();
        sndLastUpdate.setAction(CardRisks.INTENT_SYNC_LAST_UPDATE);
        if(date != null) sndLastUpdate.putExtra(CardRisks.INTENT_SYNC_LAST_UPDATE__DATE, date);
        sendBroadcast(sndLastUpdate);
    }

    private void sendIntentDiagKeyCount(int count) {
        // send intent with diag key count
        Intent sndDiagKeyCount = new Intent();
        sndDiagKeyCount.setAction(CardRisks.INTENT_SYNC_DIAG_KEY_COUNT);
        sndDiagKeyCount.putExtra(CardRisks.INTENT_SYNC_DIAG_KEY_COUNT__COUNT, count);
        sendBroadcast(sndDiagKeyCount);
    }

    private void sendIntentSyncStatusUpdateDone() {
        Intent sndSyncStatusUpdate = new Intent();
        sndSyncStatusUpdate.setAction(CardRisks.INTENT_SYNC_STATUS_SYNC);
        sndSyncStatusUpdate.putExtra(CardRisks.INTENT_SYNC_STATUS_SYNC__RUNNING, false);
        getApplicationContext().sendBroadcast(sndSyncStatusUpdate);
    }

    private void getUpdates() {
        // send last update
        sendIntentLastUpdate(sharedPreferences.getString(getString(R.string.internal_settings_key_last_scan_timestamp), null));

        // query db -> number of keys
        final Database db = Database.getInstance(this);
        db.runAsync(() -> {
            CwaDiagKeyCount diagKeyCount = db.cwaDatabase().cwaDiagKey().getCount();
            sendIntentDiagKeyCount(diagKeyCount.count);
        });

        // check if work is running
        if(!DiagKeySyncWorker.isRunning()) {
            sendIntentSyncStatusUpdateDone();
        }
    }
}
