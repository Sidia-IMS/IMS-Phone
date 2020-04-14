package com.sidia.ims.imsphone.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.sidia.ims.imsphone.notifications.NotificationsManager;
import com.sidia.ims.imsphone.service.ImsPhoneService;


public abstract class ImsPhoneReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "SP-BroadcastReceiver";
    private String savedNumber;  //because the passed incoming is only valid in ringing

    private static TelecomManager telecomManager;
    private static ComponentName componentName;
    private static PhoneAccountHandle phoneAccountHandle;


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
            Log.d("BLA", "savedNumber: " + savedNumber);
        } else {
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

            Log.d("BLA", "number: " + number);
            //createPhoneAccountHandle(context, number);
        }
    }

    private static void createPhoneAccountHandle(Context context, String number) {
        if (telecomManager == null) {
            telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            NotificationsManager.createCallNotification(context);
        }

        if (componentName == null) {
            componentName = new ComponentName(context, ImsPhoneService.class);
            phoneAccountHandle = new PhoneAccountHandle(componentName, componentName.getPackageName());
            PhoneAccount phoneAccount = PhoneAccount.builder(phoneAccountHandle, componentName.getPackageName())
                    .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED).build();

            telecomManager.registerPhoneAccount(phoneAccount);
        }

        if (phoneAccountHandle != null && number != null) {
            Bundle bundle = new Bundle();
            Uri uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
            bundle.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri);
            bundle.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
            telecomManager.addNewIncomingCall(phoneAccountHandle, bundle);
        }
    }
}
