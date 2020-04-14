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
package com.sidia.ims.imsphone.call.linphone;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.call.BandwidthManager;
import com.sidia.ims.imsphone.service.linphone.LinphoneContext;
import com.sidia.ims.imsphone.service.linphone.LinphoneManager;
import com.sidia.ims.imsphone.utils.linphone.LinphoneUtils;

import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.CallParams;
import org.linphone.core.Core;
import org.linphone.core.MediaEncryption;
import org.linphone.core.ProxyConfig;


/** Handle call updating, reinvites. */
public class CallManager {
    private static final String LOG_TAG = "SP-CallManager";
    private Context mContext;
    private BandwidthManager mBandwidthManager;

    public CallManager(Context context) {
        mContext = context;
        mBandwidthManager = new BandwidthManager();
    }

    public void destroy() {
        mBandwidthManager.destroy();
    }

    public void terminateCurrentCallOrConferenceOrAll() {
        Core core = LinphoneManager.getCore(mContext);
        Call call = core.getCurrentCall();
        if (call != null) {
            call.terminate();
        } else if (core.isInConference()) {
            core.terminateConference();
        } else {
            core.terminateAllCalls();
        }
    }

    public boolean acceptCall(Call call) {
        if (call == null) return false;

        Core core = LinphoneManager.getCore(mContext);
        CallParams params = core.createCallParams(call);

        boolean isLowBandwidthConnection =
                !LinphoneUtils.isHighBandwidthConnection(mContext);

        if (params != null) {
            params.enableLowBandwidth(isLowBandwidthConnection);
        } else {
            Log.e(LOG_TAG, "[Call Manager] Could not create call params for call");
            return false;
        }

        call.acceptWithParams(params);
        return true;
    }

    public void acceptCallUpdate(boolean accept) {
        Core core = LinphoneManager.getCore(mContext);
        Call call = core.getCurrentCall();
        if (call == null) {
            return;
        }

        CallParams params = core.createCallParams(call);
        if (accept) {
            params.enableVideo(true);
            core.enableVideoCapture(true);
            core.enableVideoDisplay(true);
        }

        call.acceptUpdate(params);
    }

    public void inviteAddress(String address, boolean forceZRTP) {
        boolean isLowBandwidthConnection =
                !LinphoneUtils.isHighBandwidthConnection(
                        LinphoneContext.instance(mContext).getApplicationContext());

        inviteAddress(address, isLowBandwidthConnection, forceZRTP);
    }

    public void newOutgoingCall(String to) {
        if (to == null) return;

        Core core = LinphoneManager.getCore(mContext);
        ProxyConfig lpc = core.getDefaultProxyConfig();
        if (mContext.getResources().getBoolean(R.bool.forbid_self_call)
                && lpc != null
//FIXME                && address.weakEqual(lpc.getIdentityAddress())) {
                && to.equals(lpc.getIdentityAddress())) {
            return;
        }

        boolean isLowBandwidthConnection =
                !LinphoneUtils.isHighBandwidthConnection(
                        LinphoneContext.instance(mContext).getApplicationContext());

        if (core.isNetworkReachable()) {
            inviteAddress(to, false, isLowBandwidthConnection);
        } else {
            Toast.makeText(
                            mContext,
                            mContext.getString(R.string.error_network_unreachable),
                            Toast.LENGTH_LONG)
                    .show();
            Log.e(LOG_TAG, "[Call Manager] Error: "
                            + mContext.getString(R.string.error_network_unreachable));
        }
    }

    public void playDtmf(ContentResolver r, char dtmf) {
        try {
            if (Settings.System.getInt(r, Settings.System.DTMF_TONE_WHEN_DIALING) == 0) {
                // audible touch disabled: don't play on speaker, only send in outgoing stream
                return;
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.e(LOG_TAG, "[Call Manager] playDtmf exception: " + e.getMessage());
        }

        LinphoneManager.getCore(mContext).playDtmf(dtmf, -1);
    }

    private void inviteAddress(String phoneNumber, boolean lowBandwidth, boolean forceZRTP) {
        Core core = LinphoneManager.getCore(mContext);

        CallParams params = core.createCallParams(null);
        mBandwidthManager.updateWithProfileSettings(params);

        params.enableVideo(false);

        if (lowBandwidth) {
            params.enableLowBandwidth(true);
            Log.d(LOG_TAG, "[Call Manager] Low bandwidth enabled in call params");
        }

        if (forceZRTP) {
            params.setMediaEncryption(MediaEncryption.ZRTP);
        }

        Address address = core.interpretUrl(phoneNumber); // InterpretUrl does normalizePhoneNumber
        if (address == null) {
            Log.e(LOG_TAG,"[Call Manager] Couldn't convert to String to Address : " + phoneNumber);
            return;
        }
        core.inviteAddressWithParams(address, params);
    }
}
