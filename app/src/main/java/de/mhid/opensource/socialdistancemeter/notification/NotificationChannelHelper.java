package de.mhid.opensource.socialdistancemeter.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import de.mhid.opensource.socialdistancemeter.R;

public class NotificationChannelHelper {
    private final static String NOTIFICATION_CHANNEL_ID = "social_distance_meter";

    private static NotificationManager notificationManager = null;
    private static NotificationChannel notificationChannel = null;

    @NonNull
    private static NotificationManager getNotificationManager(@NonNull Context ctx) {
        if(notificationManager == null) {
            notificationManager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    @NonNull
    public static String getChannelId(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(notificationChannel == null) {
                notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, ctx.getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN);
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                getNotificationManager(ctx).createNotificationChannel(notificationChannel);
            }
        }
        return NOTIFICATION_CHANNEL_ID;
    }

    private NotificationChannelHelper() {}
}
