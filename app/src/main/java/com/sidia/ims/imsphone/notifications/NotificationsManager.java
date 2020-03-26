package com.sidia.ims.imsphone.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.call.CallIncomingActivity;

public class NotificationsManager {
    private static final String CHANNEL_ID = "com.sidia.ims.imphone.notification";

    public static void createCallNotification(Context ctx) {
        NotificationManager mgr = ctx.getSystemService(NotificationManager.class);
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH);

            // We'll use the default system ringtone for our incoming call notification channel.  You can
            // use your own audio resource here.
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            channel.setSound(ringtoneUri, new AudioAttributes.Builder()
                    // Setting the AudioAttributes is important as it identifies the purpose of your
                    // notification sound.
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());

            mgr.createNotificationChannel(channel);
        }
    }

    public static void displayCallNotification(Context ctx, int notificationId, String number) {
        // Create an intent which triggers your fullscreen incoming call user interface.
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(ctx, CallIncomingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 1, intent, 0);

        // Build the notification as an ongoing high priority item; this ensures it will show as
        // a heads up notification which slides down over top of the current content.
        final Notification.Builder builder = new Notification.Builder(ctx, CHANNEL_ID);
        builder.setOngoing(true);

        // Set notification content intent to take user to fullscreen UI if user taps on the
        // notification body.
        builder.setContentIntent(pendingIntent);
        // Set full screen intent to trigger display of the fullscreen UI when the notification
        // manager deems it appropriate.
        builder.setFullScreenIntent(pendingIntent, true);

        // Setup notification content.
        builder.setSmallIcon(R.mipmap.ic_launcher_round);
        builder.setContentTitle(ctx.getString(R.string.incoming_call));
        builder.setContentText(number);

        builder.addAction(R.drawable.call_audio_start, ctx.getString(R.string.accept), null);
        builder.addAction(R.drawable.call_hangup, ctx.getString(R.string.decline), null);
        NotificationManager notificationManager = ctx.getSystemService(
                NotificationManager.class);
        notificationManager.notify(CHANNEL_ID, notificationId, builder.build());
    }
}
