package de.mhid.opensource.socialdistancemeter.services.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Date;
import java.util.TimeZone;

import de.mhid.opensource.socialdistancemeter.database.Database;

public class PurgeTokenFromDatabaseWorker extends Worker {
    public PurgeTokenFromDatabaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final int utcOffset = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();
        final long utcTime = new Date().getTime() - utcOffset;
        final long rollingTimestamp = utcTime / (10 * 60 * 1000);

        final long rollingTimestampTodayMidnight = rollingTimestamp - (rollingTimestamp % 144);
        final long rollingTimestamp14DaysAgo = rollingTimestampTodayMidnight - 14 * 144;

        Database db = Database.getInstance(getApplicationContext());
        db.runSync((Database.RunnableWithReturn<Void>) () -> {
            db.cwaDatabase().cwaToken().purgeOldTokens(rollingTimestamp14DaysAgo);
            return null;
        });

        return Result.success();
    }
}
