package com.sidia.ims.imsphone.telephony;


import android.telecom.Connection;
import android.util.Log;

public class ImsPhoneConnection extends Connection {
    @Override
    public void onShowIncomingCallUi() {
        Log.d("BLA", "onShowIncomingCallUi");
        super.onShowIncomingCallUi();
    }
}
