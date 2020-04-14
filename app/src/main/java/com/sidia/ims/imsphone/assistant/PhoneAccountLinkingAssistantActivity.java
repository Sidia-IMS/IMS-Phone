package com.sidia.ims.imsphone.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.service.linphone.LinphoneManager;

import org.linphone.core.AccountCreator;
import org.linphone.core.AccountCreatorListenerStub;
import org.linphone.core.Address;
import org.linphone.core.AuthInfo;
import org.linphone.core.Core;
import org.linphone.core.DialPlan;
import org.linphone.core.ProxyConfig;

import androidx.annotation.Nullable;

public class PhoneAccountLinkingAssistantActivity extends AssistantActivity implements View.OnClickListener{
    private static final String LOG_TAG = "SP-AccountLinking";
    private Button btnLink;
    private EditText mPrefix, mPhoneNumber;
    private AccountCreatorListenerStub mListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_phone_account_linking);

        if (getIntent() != null && getIntent().hasExtra("AccountNumber")) {
            int proxyConfigIndex = getIntent().getExtras().getInt("AccountNumber");
            Core core = LinphoneManager.getCore(this);
            if (core == null) {
                Log.e(LOG_TAG,"[Account Linking Assistant] Core not available");
                unexpectedError();
                return;
            }

            ProxyConfig[] proxyConfigs = core.getProxyConfigList();
            if (proxyConfigIndex >= 0 && proxyConfigIndex < proxyConfigs.length) {
                ProxyConfig mProxyConfig = proxyConfigs[proxyConfigIndex];
                AccountCreator accountCreator = getAccountCreator();

                Address identity = mProxyConfig.getIdentityAddress();
                if (identity == null) {
                    Log.e(LOG_TAG,"[Account Linking Assistant] Proxy doesn't have an identity address");
                    unexpectedError();
                    return;
                }
                if (!mProxyConfig.getDomain().equals(getString(R.string.default_domain))) {
                    Log.e(LOG_TAG,
                            "[Account Linking Assistant] Can't link account on domain "
                                    + mProxyConfig.getDomain());
                    unexpectedError();
                    return;
                }
                accountCreator.setUsername(identity.getUsername());

                AuthInfo authInfo = mProxyConfig.findAuthInfo();
                if (authInfo == null) {
                    Log.e(LOG_TAG,"[Account Linking Assistant] Auth info not found");
                    unexpectedError();
                    return;
                }
                accountCreator.setHa1(authInfo.getHa1());
                accountCreator.setAlgorithm((authInfo.getAlgorithm()));
            } else {
                Log.e(LOG_TAG,
                        "[Account Linking Assistant] Proxy config index out of bounds: "
                                + proxyConfigIndex);
                unexpectedError();
                return;
            }
        } else {
            Log.e(LOG_TAG,"[Account Linking Assistant] Proxy config index not found");
            unexpectedError();
            return;
        }

        btnLink = findViewById(R.id.assistant_link);
        btnLink.setOnClickListener(this);
        mPrefix = findViewById(R.id.dial_code);
        mPhoneNumber = findViewById(R.id.phone_number);

        mListener =
                new AccountCreatorListenerStub() {
                    @Override
                    public void onIsAliasUsed(
                            AccountCreator creator, AccountCreator.Status status, String resp) {
                        Log.i(LOG_TAG,
                                "[Phone Account Linking Assistant] onIsAliasUsed status is "
                                        + status);
                        if (status.equals(AccountCreator.Status.AliasNotExist)) {
                            status = getAccountCreator().linkAccount();
                            if (status != AccountCreator.Status.RequestOk) {
                                Log.e(LOG_TAG,
                                        "[Phone Account Linking Assistant] linkAccount returned "
                                                + status);
                                enableButtonsAndFields(true);
                                showGenericErrorDialog(status);
                            }
                        } else {
                            if (status.equals(AccountCreator.Status.AliasIsAccount)
                                    || status.equals(AccountCreator.Status.AliasExist)) {
                                showAccountAlreadyExistsDialog();
                            } else {
                                showGenericErrorDialog(status);
                            }
                            enableButtonsAndFields(true);
                        }
                    }

                    @Override
                    public void onLinkAccount(
                            AccountCreator creator, AccountCreator.Status status, String resp) {
                        Log.i(LOG_TAG,
                                "[Phone Account Linking Assistant] onLinkAccount status is "
                                        + status);
                        if (status.equals(AccountCreator.Status.RequestOk)) {
                            Intent intent =
                                    new Intent(
                                            PhoneAccountLinkingAssistantActivity.this,
                                            PhoneAccountValidationAssistantActivity.class);
                            intent.putExtra("isLinkingVerification", true);
                            startActivity(intent);
                        } else {
                            enableButtonsAndFields(true);
                            showGenericErrorDialog(status);
                        }
                    }
                };
    }

    private void enableButtonsAndFields(boolean enable) {
        mPrefix.setEnabled(enable);
        mPhoneNumber.setEnabled(enable);
    }

    private void unexpectedError() {
        Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Core core = LinphoneManager.getCore(this);
        if (core != null) {
            reloadLinphoneAccountCreatorConfig();
        }

        getAccountCreator().addListener(mListener);

        String phoneNumber = getDevicePhoneNumber();
        if (phoneNumber != null) {
            mPhoneNumber.setText(phoneNumber);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getAccountCreator().removeListener(mListener);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.assistant_link:
                enableButtonsAndFields(false);

                AccountCreator.Status status = getAccountCreator().isAliasUsed();
                if (status != AccountCreator.Status.RequestOk) {
                    Log.e(LOG_TAG,
                            "[Phone Account Linking Assistant] isAliasUsed returned "
                                    + status);
                    enableButtonsAndFields(true);
                    showGenericErrorDialog(status);
                }
                break;
            default:
                break;
        }
    }
}
