package com.sidia.ims.imsphone.service.linphone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.sidia.ims.imsphone.notifications.NotificationsManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class LinphoneService extends Service {
    private  static final String LOG_TAG = "SP-LinphoneService";
    private static LinphoneService sInstance;
    private boolean misLinphoneContextOwned;

    @Override
    public void onCreate() {
        super.onCreate();

        misLinphoneContextOwned = false;
        if (!LinphoneContext.isReady()) {
            new LinphoneContext(getApplicationContext());
            misLinphoneContextOwned = true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationsManager.createDefaultNotification(this, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,  NotificationsManager.DEFAULT_CHANNEL_ID)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            startForeground(27, builder.build());
        }
        Log.i(LOG_TAG, "[Service] Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        boolean isPush = false;
        if (intent != null && intent.getBooleanExtra("PushNotification", false)) {
            Log.i(LOG_TAG,"[Service] [Push Notification] LinphoneService started because of a push");
            isPush = true;
        }

        if (sInstance != null) {
            Log.w(LOG_TAG,"[Service] Attempt to start the LinphoneService but it is already running !");
            return START_STICKY;
        }
        sInstance = this;

        if (misLinphoneContextOwned) {
            LinphoneContext.instance(this).start(isPush);
        } else {
            LinphoneContext.instance(this).updateContext(this);
        }

        Log.i(LOG_TAG,"[Service] Started");
        return  START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isReady() {
        return sInstance != null;
    }

    public static LinphoneService instance() {
        if (isReady()) return sInstance;

        throw new RuntimeException("LinphoneService not instantiated yet");
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG,"[Service] Destroying");

        sInstance = null;
        super.onDestroy();
    }
}
