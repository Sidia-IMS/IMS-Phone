package com.sidia.ims.imsphone.telephony;


import android.content.Context;
import android.telecom.Connection;
import android.util.Log;

import com.sidia.ims.imsphone.notifications.NotificationsManager;

public class ImsPhoneConnection extends Connection {
    private Context context;
    private String number;

    public ImsPhoneConnection (Context ctx, String number) {
        this.context = ctx;
        this.number = number;
    }

    @Override
    public void onShowIncomingCallUi() {
        NotificationsManager.displayCallNotification(context, 37, number);
    }
}
