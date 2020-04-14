package com.sidia.ims.imsphone.service;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.telecom.Call;
import android.util.Log;

import com.sidia.ims.imsphone.call.CallOutgoingActivity;
import com.sidia.ims.imsphone.notifications.NotificationsManager;
import android.telecom.InCallService;
import com.sidia.ims.imsphone.telephony.OngoingCall;

public class ImsPhoneInCallServiceImpl extends InCallService {
    private static final String LOG_TAG = "SP-InCallService";
    private static final int NOTIFICATION_ID = 37;

    @Override
    public void onCallAdded(Call call) {
        Log.d(LOG_TAG, "onCallAdded");
        Log.d(LOG_TAG, "state: " + call.getState());

        boolean isConnecting = call.getState() == Call.STATE_CONNECTING;
        boolean isOutgoing = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isOutgoing =  call.getDetails().getCallDirection() == Call.Details.DIRECTION_OUTGOING;
        }

        OngoingCall.setCall(call);
        if (!isConnecting && !isOutgoing) {
            NotificationsManager.createCallNotification(this);
            NotificationsManager.displayCallNotification(this, NOTIFICATION_ID,
                    call.getDetails().getHandle().getSchemeSpecificPart());
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClass(this, CallOutgoingActivity.class);
            this.startActivity(intent);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        OngoingCall.setCall(null);
    }
}
