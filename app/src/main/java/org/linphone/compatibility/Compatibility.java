/*
 * Copyright (c) 2010-2019 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.compatibility;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import org.linphone.mediastream.Version;

public class Compatibility {
    public static final String INTENT_NOTIF_ID = "NOTIFICATION_ID";
    public static final String INTENT_HANGUP_CALL_NOTIF_ACTION = "org.linphone.HANGUP_CALL_ACTION";
    public static final String INTENT_ANSWER_CALL_NOTIF_ACTION = "org.linphone.ANSWER_CALL_ACTION";

    public static String getDeviceName(Context context) {
        if (Version.sdkAboveOrEqual(25)) {
            return ApiTwentySixPlus.getDeviceName(context);
        }

        String name = null;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            name = adapter.getName();
        }

        if (name == null) {
            name = Settings.Secure.getString(context.getContentResolver(), "bluetooth_name");
        }
        if (name == null) {
            name = Build.MANUFACTURER + " " + Build.MODEL;
        }
        return name;
    }

    public static void createNotificationChannels(Context context) {
        if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
            ApiTwentySixPlus.createServiceChannel(context);
        }
    }

    public static Notification createMissedCallNotification(
            Context context, String title, String text, PendingIntent intent, int count) {
        if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
            return ApiTwentySixPlus.createMissedCallNotification(
                    context, title, text, intent, count);
        }
        return ApiTwentyOnePlus.createMissedCallNotification(context, title, text, intent, count);
    }

    public static Notification createInCallNotification(
            Context context,
            int callId,
            String msg,
            int iconID,
            Bitmap contactIcon,
            String contactName,
            PendingIntent intent) {
        if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
            return ApiTwentySixPlus.createInCallNotification(
                    context, callId, msg, iconID, contactIcon, contactName, intent);
        } else if (Version.sdkAboveOrEqual(Version.API24_NOUGAT_70)) {
            return ApiTwentyFourPlus.createInCallNotification(
                    context, callId, msg, iconID, contactIcon, contactName, intent);
        }
        return ApiTwentyOnePlus.createInCallNotification(
                context, msg, iconID, contactIcon, contactName, intent);
    }

    public static Notification createIncomingCallNotification(
            Context context,
            int callId,
            Bitmap contactIcon,
            String contactName,
            String sipUri,
            PendingIntent intent) {
        if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
            return ApiTwentySixPlus.createIncomingCallNotification(
                    context, callId, contactIcon, contactName, sipUri, intent);
        } else if (Version.sdkAboveOrEqual(Version.API24_NOUGAT_70)) {
            return ApiTwentyFourPlus.createIncomingCallNotification(
                    context, callId, contactIcon, contactName, sipUri, intent);
        }
        return ApiTwentyOnePlus.createIncomingCallNotification(
                context, contactIcon, contactName, sipUri, intent);
    }

    public static Notification createNotification(
            Context context,
            String title,
            String message,
            int icon,
            int iconLevel,
            Bitmap largeIcon,
            PendingIntent intent,
            int priority,
            boolean ongoing) {
        if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
            return ApiTwentySixPlus.createNotification(
                    context, title, message, icon, iconLevel, largeIcon, intent, priority, ongoing);
        }
        return ApiTwentyOnePlus.createNotification(
                context, title, message, icon, iconLevel, largeIcon, intent, priority, ongoing);
    }

    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    public static void startService(Context context, Intent intent) {
        if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
            ApiTwentySixPlus.startService(context, intent);
        } else {
            context.startService(intent);
        }
    }

    public static void setFragmentTransactionReorderingAllowed(
            FragmentTransaction transaction, boolean allowed) {
        if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
            ApiTwentySixPlus.setFragmentTransactionReorderingAllowed(transaction, allowed);
        }
    }

    public static boolean isAppUserRestricted(Context context) {
        if (Version.sdkAboveOrEqual(Version.API28_PIE_90)) {
            return ApiTwentyEightPlus.isAppUserRestricted(context);
        }
        return false;
    }

    public static int getAppStandbyBucket(Context context) {
        if (Version.sdkAboveOrEqual(Version.API28_PIE_90)) {
            return ApiTwentyEightPlus.getAppStandbyBucket(context);
        }
        return 0;
    }

    public static String getAppStandbyBucketNameFromValue(int bucket) {
        if (Version.sdkAboveOrEqual(Version.API28_PIE_90)) {
            return ApiTwentyEightPlus.getAppStandbyBucketNameFromValue(bucket);
        }
        return null;
    }

    public static void setShowWhenLocked(Activity activity, boolean enable) {
        if (Version.sdkStrictlyBelow(Version.API27_OREO_81)) {
            ApiTwentyOnePlus.setShowWhenLocked(activity, enable);
        }
    }

    public static void setTurnScreenOn(Activity activity, boolean enable) {
        if (Version.sdkStrictlyBelow(Version.API27_OREO_81)) {
            ApiTwentyOnePlus.setTurnScreenOn(activity, enable);
        }
    }

    public static void enterPipMode(Activity activity) {
        if (Version.sdkAboveOrEqual(Version.API26_O_80)) {
            ApiTwentySixPlus.enterPipMode(activity);
        }
    }

    public static Notification.Action getCallAnswerAction(Context context, int callId) {
        if (Version.sdkAboveOrEqual(Version.API29_ANDROID_10)) {
            return ApiTwentyNinePlus.getCallAnswerAction(context, callId);
        } else if (Version.sdkAboveOrEqual(Version.API24_NOUGAT_70)) {
            return ApiTwentyFourPlus.getCallAnswerAction(context, callId);
        }
        return null;
    }

    public static Notification.Action getCallDeclineAction(Context context, int callId) {
        if (Version.sdkAboveOrEqual(Version.API29_ANDROID_10)) {
            return ApiTwentyNinePlus.getCallDeclineAction(context, callId);
        } else if (Version.sdkAboveOrEqual(Version.API24_NOUGAT_70)) {
            return ApiTwentyFourPlus.getCallDeclineAction(context, callId);
        }
        return null;
    }

    public static StatusBarNotification[] getActiveNotifications(NotificationManager manager) {
        if (Version.sdkAboveOrEqual(Version.API23_MARSHMALLOW_60)) {
            return ApiTwentyThreePlus.getActiveNotifications(manager);
        }

        return new StatusBarNotification[0];
    }
}
