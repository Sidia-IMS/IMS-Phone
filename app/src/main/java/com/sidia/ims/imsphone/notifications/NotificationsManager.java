package com.sidia.ims.imsphone.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.activities.MainActivity;
import com.sidia.ims.imsphone.call.CallIncomingActivity;
import com.sidia.ims.imsphone.service.linphone.LinphoneManager;
import com.sidia.ims.imsphone.service.linphone.LinphoneService;

import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.ChatMessage;
import org.linphone.core.ChatRoom;
import org.linphone.core.ChatRoomCapabilities;
import org.linphone.core.Content;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Reason;

import java.io.File;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationsManager {
    private static final String LOG_TAG = "SP-Notification";
    public static final String CALL_CHANNEL_ID = "com.sidia.ims.imphone.call_notification";
    public static final String DEFAULT_CHANNEL_ID = "com.sidia.ims.imphone.notification";
    private static final int MISSED_CALLS_NOTIF_ID = 2;

    private final Context mContext;
    private final NotificationManager mNM;

    public NotificationsManager(Context context) {
        mContext = context;
        mNM = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
    }

    public static void createDefaultNotification(Context ctx, int importance) {
        NotificationManager mgr = ctx.getSystemService(NotificationManager.class);
        if (mgr.getNotificationChannel(DEFAULT_CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID,
                    ctx.getString(R.string.app_name), importance);
            mgr.createNotificationChannel(channel);
        }
    }

    public static void createCallNotification(Context ctx) {
        NotificationManager mgr = ctx.getSystemService(NotificationManager.class);
        if (mgr.getNotificationChannel(CALL_CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CALL_CHANNEL_ID, "Incoming Calls",
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
        final Notification.Builder builder = new Notification.Builder(ctx, CALL_CHANNEL_ID);
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
        notificationManager.notify(CALL_CHANNEL_ID, notificationId, builder.build());
    }

    public void displayMissedCallNotification(Call call) {
        Intent missedCallNotifIntent = new Intent(mContext, MainActivity.class);
        addFlagsToIntent(missedCallNotifIntent);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        mContext,
                        MISSED_CALLS_NOTIF_ID,
                        missedCallNotifIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        int missedCallCount = LinphoneManager.getCore(mContext).getMissedCallsCount();
        String body;
        if (missedCallCount > 1) {
            body =
                    mContext.getString(R.string.missed_calls_notif_body)
                            .replace("%i", String.valueOf(missedCallCount));
            Log.i(LOG_TAG,"[Notifications Manager] Creating missed calls notification");
        } else {
            Address address = call.getRemoteAddress();
            body = address.getDisplayName();
            if (body == null) {
                body = address.asStringUriOnly();
            }

            Log.i(LOG_TAG,"[Notifications Manager] Creating missed call notification");
        }

        Notification notif = createMissedCallNotification(
                        mContext,
                        mContext.getString(R.string.missed_calls_notif_title),
                        body,
                        pendingIntent,
                        missedCallCount);
        sendNotification(MISSED_CALLS_NOTIF_ID, notif);
    }

    public static Notification createMissedCallNotification(
            Context context, String title, String text, PendingIntent intent, int count) {
        Notification.Builder builder = new Notification.Builder(context, CALL_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.call_status_missed)
                .setAutoCancel(true)
                .setContentIntent(intent)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setCategory(Notification.CATEGORY_EVENT)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPriority(Notification.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setColor(context.getColor(R.color.notification_led_color))
                .setNumber(count);

        return builder.build();
    }

    public void sendNotification(int id, Notification notif) {
        Log.i(LOG_TAG,"[Notifications Manager] Notifying " + id);
        mNM.notify(id, notif);
    }

    private void addFlagsToIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }
}
