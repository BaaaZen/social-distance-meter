package de.mhid.opensource.cwadetails.services;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import de.mhid.opensource.cwadetails.diagkeys.DiagKeySyncWorker;

public class DiagKeySyncService extends Service {
    private final static String PERIODIC_WORK_NAME = "periodic_diag_key_update_work";
    private final static String ONE_TIME_WORK_NAME = "one_time_diag_key_update_work";

    public final static String INTENT_START_DIAG_KEY_UPDATE = "start_diag_key_update";

    @Override
    public void onCreate() {
        super.onCreate();

        initPeriodicWork();
    }

    private void initPeriodicWork() {
        Constraints.Builder constraintBuilder = new Constraints.Builder()
                .setRequiresBatteryNotLow(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            constraintBuilder = constraintBuilder.setRequiresDeviceIdle(true);
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
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(DiagKeySyncWorker.class)
                    .build();

        WorkManager.getInstance(this)
                .enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, workRequest);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction() != null && intent.getAction().equals(INTENT_START_DIAG_KEY_UPDATE)) {
            startOneTimeWork();
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
}
