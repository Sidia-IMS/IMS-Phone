package com.sidia.ims.imsphone.receivers;

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

import com.sidia.ims.imsphone.service.ImsPhoneService;

import java.util.Date;

public abstract class ImsPhoneReceiver extends BroadcastReceiver {
    public static final String EXTRA_PHONE_ACCOUNT = "KEY_PHONE_ACCOUNT";

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private Date callStartTime;
    private boolean isIncoming;
    private String savedNumber;  //because the passed incoming is only valid in ringing

    private static TelecomManager telecomManager;
    private static ComponentName componentName;
    private static PhoneAccountHandle phoneAccountHandle;
    private static PhoneAccount phoneAccount;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
            Log.d("BLA", "savedNumber: " + String.valueOf(savedNumber));
        } else {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

            Log.d("BLA", "number: " + String.valueOf(number));
            int state = 0;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }

            createPhoneAccountHandle(context, number);
            onCallStateChanged(context, state, number);
        }
    }

    //Derived classes should override these to respond to specific events of interest
    protected abstract void onIncomingCallStarted(Context ctx, String number, Date start);
    protected abstract void onOutgoingCallStarted(Context ctx, String number, Date start);
    protected abstract void onIncomingCallEnded(Context ctx, String number, Date start, Date end);
    protected abstract void onOutgoingCallEnded(Context ctx, String number, Date start, Date end);
    protected abstract void onMissedCall(Context ctx, String number, Date start);

    //Deals with actual events

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(Context context, int state, String number) {
        if (lastState == state) {
            //No change, debounce extras
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;
                onIncomingCallStarted(context, number, callStartTime);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    callStartTime = new Date();
                    onOutgoingCallStarted(context, savedNumber, callStartTime);
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    onMissedCall(context, savedNumber, callStartTime);
                } else if(isIncoming) {
                    onIncomingCallEnded(context, savedNumber, callStartTime, new Date());
                } else {
                    onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());
                }
                break;
        }
        lastState = state;
    }

    private static void createPhoneAccountHandle(Context context, String number) {
        if (telecomManager == null) {
            telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        }

        if (componentName == null) {
            componentName = new ComponentName(context, ImsPhoneService.class);
            phoneAccountHandle = new PhoneAccountHandle(componentName, componentName.getPackageName());
            phoneAccount = PhoneAccount.builder(phoneAccountHandle, componentName.getPackageName())
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
