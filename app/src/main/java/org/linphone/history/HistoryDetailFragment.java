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
package org.linphone.history;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.Arrays;
import java.util.List;
import org.linphone.LinphoneManager;
import org.linphone.R;
import org.linphone.activities.MainActivity;
import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.CallLog;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Factory;
import org.linphone.core.ProxyConfig;
import org.linphone.utils.LinphoneUtils;

public class HistoryDetailFragment extends Fragment {
    private TextView mContactName, mContactAddress;
    private String mSipUri, mDisplayName;
    private RelativeLayout mWaitLayout;
    private ListView mLogsList;
    private CoreListenerStub mListener;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mSipUri = getArguments().getString("SipUri");
        mDisplayName = getArguments().getString("DisplayName");

        View view = inflater.inflate(R.layout.history_detail, container, false);

        mWaitLayout = view.findViewById(R.id.waitScreen);
        mWaitLayout.setVisibility(View.GONE);

        ImageView dialBack = view.findViewById(R.id.call);
        dialBack.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((MainActivity) getActivity()).newOutgoingCall(mSipUri);
                    }
                });

        ImageView back = view.findViewById(R.id.back);
        back.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((HistoryActivity) getActivity()).goBack();
                    }
                });
        back.setVisibility(
                getResources().getBoolean(R.bool.isTablet) ? View.INVISIBLE : View.VISIBLE);

        mContactName = view.findViewById(R.id.contact_name);
        mContactAddress = view.findViewById(R.id.contact_address);

        mLogsList = view.findViewById(R.id.logs_list);

        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onCallStateChanged(
                            Core core, Call call, Call.State state, String message) {
                        if (state == Call.State.End || state == Call.State.Error) {
                            displayHistory();
                        }
                    }
                };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        LinphoneManager.getCore().addListener(mListener);
        displayHistory();
    }

    @Override
    public void onPause() {
        LinphoneManager.getCore().removeListener(mListener);

        super.onPause();
    }

    private void displayHistory() {
        if (mSipUri != null) {
            Address address = Factory.instance().createAddress(mSipUri);

            Core core = LinphoneManager.getCore();
            if (address != null && core != null) {
                address.clean();
                ProxyConfig proxyConfig = core.getDefaultProxyConfig();
                CallLog[] logs;
                if (proxyConfig != null) {
                    logs = core.getCallHistory(address, proxyConfig.getIdentityAddress());
                } else {
                    logs = core.getCallHistoryForAddress(address);
                }
                List<CallLog> logsList = Arrays.asList(logs);
                mLogsList.setAdapter(
                        new HistoryLogAdapter(
                                getActivity(), R.layout.history_detail_cell, logsList));

                mContactAddress.setText(LinphoneUtils.getDisplayableAddress(address));

                if (mDisplayName == null) {
                    mDisplayName = LinphoneUtils.getAddressDisplayName(address);
                }

                mContactName.setText(mDisplayName);
            } else {
                mContactAddress.setText(mSipUri);
                mContactName.setText(
                        mDisplayName == null
                                ? LinphoneUtils.getAddressDisplayName(mSipUri)
                                : mDisplayName);
            }
        }
    }
}
