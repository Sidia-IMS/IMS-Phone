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
package org.linphone.dialer;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import org.linphone.LinphoneManager;
import org.linphone.R;
import org.linphone.activities.MainActivity;
import org.linphone.call.views.CallButton;
import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.tools.Log;
import org.linphone.dialer.views.AddressText;
import org.linphone.dialer.views.Digit;
import org.linphone.dialer.views.EraseButton;

public class DialerActivity extends MainActivity implements AddressText.AddressChangedListener {
    private static final String ACTION_CALL_LINPHONE = "org.linphone.intent.action.CallLaunched";

    private AddressText mAddress;
    private CallButton mStartCall;
    private ImageView mBackToCall;

    private boolean mIsTransfer;
    private CoreListenerStub mListener;
    private boolean mInterfaceLoaded;
    private String mAddressToCallOnLayoutReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInterfaceLoaded = false;
        // Uses the fragment container layout to inflate the dialer view instead of using a fragment
        new AsyncLayoutInflater(this)
                .inflate(
                        R.layout.dialer,
                        null,
                        new AsyncLayoutInflater.OnInflateFinishedListener() {
                            @Override
                            public void onInflateFinished(
                                    @NonNull View view, int resid, @Nullable ViewGroup parent) {
                                LinearLayout fragmentContainer =
                                        findViewById(R.id.fragmentContainer);
                                LinearLayout.LayoutParams params =
                                        new LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT);
                                fragmentContainer.addView(view, params);
                                initUI(view);
                                mInterfaceLoaded = true;
                                if (mAddressToCallOnLayoutReady != null) {
                                    mAddress.setText(mAddressToCallOnLayoutReady);
                                    mAddressToCallOnLayoutReady = null;
                                }
                            }
                        });

        if (isTablet()) {
            findViewById(R.id.fragmentContainer2).setVisibility(View.GONE);
        }

        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onCallStateChanged(
                            Core core, Call call, Call.State state, String message) {
                        updateLayout();
                    }
                };

        // On dialer we ask for all permissions
        mPermissionsToHave =
                new String[] {
                    // This one is to allow floating notifications
                    Manifest.permission.SYSTEM_ALERT_WINDOW,
                    // Required starting Android 9 to be able to start a foreground service
                    "android.permission.FOREGROUND_SERVICE",
                };

        mIsTransfer = false;
        if (getIntent() != null) {
            mIsTransfer = getIntent().getBooleanExtra("isTransfer", false);
        }

        handleIntentParams(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntentParams(intent);

        if (intent != null) {
            mIsTransfer = intent.getBooleanExtra("isTransfer", mIsTransfer);
            if (mAddress != null && intent.getStringExtra("SipUri") != null) {
                mAddress.setText(intent.getStringExtra("SipUri"));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDialerSelected.setVisibility(View.VISIBLE);
        Core core = LinphoneManager.getCore();
        if (core != null) {
            core.addListener(mListener);
        }

        if (mInterfaceLoaded) {
            updateLayout();
        }
    }

    @Override
    protected void onPause() {
        Core core = LinphoneManager.getCore();
        if (core != null) {
            core.removeListener(mListener);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mInterfaceLoaded) {
            mAddress = null;
            mStartCall = null;
            mBackToCall = null;
        }
        if (mListener != null) mListener = null;

        super.onDestroy();
    }

    private void initUI(View view) {
        mAddress = view.findViewById(R.id.address);
        mAddress.setAddressListener(this);

        EraseButton erase = view.findViewById(R.id.erase);
        erase.setAddressWidget(mAddress);

        mStartCall = view.findViewById(R.id.start_call);
        mStartCall.setAddressWidget(mAddress);

        mBackToCall = view.findViewById(R.id.back_to_call);
        mBackToCall.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        goBackToCall();
                    }
                });

        if (getIntent() != null) {
            mAddress.setText(getIntent().getStringExtra("SipUri"));
        }

        setUpNumpad(view);
        updateLayout();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("isTransfer", mIsTransfer);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mIsTransfer = savedInstanceState.getBoolean("isTransfer");
    }

    @Override
    public void onAddressChanged() {
        // FIXME
    }

    private void updateLayout() {
        Core core = LinphoneManager.getCore();
        if (core == null) {
            return;
        }

        boolean atLeastOneCall = core.getCallsNb() > 0;
        mStartCall.setVisibility(atLeastOneCall ? View.GONE : View.VISIBLE);

        if (!atLeastOneCall) {
            if (core.getVideoActivationPolicy().getAutomaticallyInitiate()) {
                mStartCall.setImageResource(R.drawable.call_video_start);
            } else {
                mStartCall.setImageResource(R.drawable.call_audio_start);
            }
        }

        mBackToCall.setVisibility(atLeastOneCall ? View.VISIBLE : View.GONE);
    }

    private void handleIntentParams(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        String addressToCall = null;
        if (ACTION_CALL_LINPHONE.equals(action)
                && (intent.getStringExtra("NumberToCall") != null)) {
            String numberToCall = intent.getStringExtra("NumberToCall");
            Log.i("[Dialer] ACTION_CALL_LINPHONE with number: " + numberToCall);
            LinphoneManager.getCallManager().newOutgoingCall(numberToCall, null);
        } else {
            Uri uri = intent.getData();
            if (uri != null) {
                Log.i("[Dialer] Intent data is: " + uri.toString());
                if (Intent.ACTION_CALL.equals(action)) {
                    String dataString = intent.getDataString();

                    try {
                        addressToCall = URLDecoder.decode(dataString, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.e("[Dialer] Unable to decode URI " + dataString);
                        addressToCall = dataString;
                    }

                    if (addressToCall.startsWith("sip:")) {
                        addressToCall = addressToCall.substring("sip:".length());
                    } else if (addressToCall.startsWith("tel:")) {
                        addressToCall = addressToCall.substring("tel:".length());
                    }
                    Log.i("[Dialer] ACTION_CALL with number: " + addressToCall);
                }
            } else {
                Log.w("[Dialer] Intent data is null for action " + action);
            }
        }

        if (addressToCall != null) {
            if (mAddress != null) {
                mAddress.setText(addressToCall);
            } else {
                mAddressToCallOnLayoutReady = addressToCall;
            }
        }
    }

    private void setUpNumpad(View view) {
        if (view == null) return;
        for (Digit v : retrieveChildren((ViewGroup) view, Digit.class)) {
            v.setAddressWidget(mAddress);
        }
    }

    private <T> Collection<T> retrieveChildren(ViewGroup viewGroup, Class<T> clazz) {
        final Collection<T> views = new ArrayList<>();
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View v = viewGroup.getChildAt(i);
            if (v instanceof ViewGroup) {
                views.addAll(retrieveChildren((ViewGroup) v, clazz));
            } else {
                if (clazz.isInstance(v)) views.add(clazz.cast(v));
            }
        }
        return views;
    }
}
