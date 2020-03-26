package com.sidia.ims.imsphone.telephony;

import android.telecom.Call;
import android.telecom.VideoProfile;

public class OngoingCall {
    private static Call call;

    public static Call getCall() {
        return call;
    }

    public static void setCall(Call call) {
        OngoingCall.call = call;
    }

    public static void  answer() {
        if (call != null) {
            call.answer(VideoProfile.STATE_AUDIO_ONLY);
        }
    }

    public static void hangup() {
        if (call != null) {
            call.disconnect();
        }
    }
}
