package com.sidia.ims.imsphone.service;

import android.telecom.Call;
import android.util.Log;

import com.sidia.ims.imsphone.notifications.NotificationsManager;
import android.telecom.InCallService;
import com.sidia.ims.imsphone.telephony.OngoingCall;

public class ImsPhoneInCallServiceImpl extends InCallService {
    private static final String LOG_TAG = "SP-InCallService";

    @Override
    public void onCallAdded(Call call) {
        Log.d(LOG_TAG, "onCallAdded");

        OngoingCall.setCall(call);
        NotificationsManager.createCallNotification(this);
        NotificationsManager.displayCallNotification(this, 37,
                call.getDetails().getHandle().getSchemeSpecificPart());
    }

    @Override
    public void onCallRemoved(Call call) {
        OngoingCall.setCall(null);
    }
}
