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
package org.linphone.assistant;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import org.linphone.LinphoneManager;
import org.linphone.R;
import org.linphone.core.ConfiguringState;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.settings.LinphonePreferences;

public class RemoteConfigurationAssistantActivity extends AssistantActivity {
    private TextView mFetchAndApply;
    private EditText mRemoteConfigurationUrl;
    private RelativeLayout mWaitLayout;

    private CoreListenerStub mListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.assistant_remote_configuration);

        mWaitLayout = findViewById(R.id.waitScreen);
        mWaitLayout.setVisibility(View.GONE);

        mFetchAndApply = findViewById(R.id.fetch_and_apply_remote_configuration);
        mFetchAndApply.setEnabled(false);
        mFetchAndApply.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String url = mRemoteConfigurationUrl.getText().toString();
                        if (Patterns.WEB_URL.matcher(url).matches()) {
                            mWaitLayout.setVisibility(View.VISIBLE);
                            mFetchAndApply.setEnabled(false);
                            LinphonePreferences.instance().setRemoteProvisioningUrl(url);
                            Core core = LinphoneManager.getCore();
                            if (core != null) {
                                core.getConfig().sync();
                                core.addListener(mListener);
                            }
                            LinphoneManager.getInstance().restartCore();
                        } else {
                            // TODO improve error text
                            Toast.makeText(
                                            RemoteConfigurationAssistantActivity.this,
                                            getString(R.string.remote_provisioning_failure),
                                            Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                });

        mRemoteConfigurationUrl = findViewById(R.id.remote_configuration_url);
        mRemoteConfigurationUrl.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        mFetchAndApply.setEnabled(!s.toString().isEmpty());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
        mRemoteConfigurationUrl.setText(LinphonePreferences.instance().getRemoteProvisioningUrl());

        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onConfiguringStatus(
                            Core core, ConfiguringState status, String message) {
                        core.removeListener(mListener);
                        mWaitLayout.setVisibility(View.GONE);
                        mFetchAndApply.setEnabled(true);

                        if (status == ConfiguringState.Successful) {
                            LinphonePreferences.instance().firstLaunchSuccessful();
                            goToLinphoneActivity();
                        } else if (status == ConfiguringState.Failed) {
                            Toast.makeText(
                                            RemoteConfigurationAssistantActivity.this,
                                            getString(R.string.remote_provisioning_failure),
                                            Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                };
    }
}
