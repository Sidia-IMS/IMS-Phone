package com.sidia.ims.imsphone.service;

import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import com.sidia.ims.imsphone.telephony.ImsPhoneConnection;

public class ImsPhoneService extends ConnectionService {

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        if (request.getExtras() == null) {
            return Connection.createFailedConnection(new DisconnectCause(DisconnectCause.ERROR, "No extras on request."));
        }

        Log.d("BLA", "onCreateIncomingConnection");
        ImsPhoneConnection connection = new ImsPhoneConnection();
        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        connection.setInitializing();
        connection.setActive();
        return connection;
    }
}