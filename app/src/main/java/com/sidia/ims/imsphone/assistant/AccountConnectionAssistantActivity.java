package com.sidia.ims.imsphone.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.service.linphone.LinphoneManager;

import org.linphone.core.AccountCreator;
import org.linphone.core.AccountCreatorListenerStub;
import org.linphone.core.Core;

import androidx.annotation.Nullable;

public class AccountConnectionAssistantActivity extends AssistantActivity implements View.OnClickListener {
    private static final String LOG_TAG = "SP-AccountConnection";
    private EditText mPrefix, mPhoneNumber;
    private TextView mError;
    private Button btnConnect;

    private AccountCreatorListenerStub mListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_account_connection);

        btnConnect = findViewById(R.id.assistant_btn_login);
        btnConnect.setOnClickListener(this);
        btnConnect.setEnabled(false);

        mPrefix = findViewById(R.id.dial_code);
        mError = findViewById(R.id.phone_number_error);

        mPhoneNumber = findViewById(R.id.phone_number);
        mPhoneNumber.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        updateConnectButtonAndDisplayError();
                    }
                });

        mListener =
                new AccountCreatorListenerStub() {
                    @Override
                    public void onRecoverAccount(
                            AccountCreator creator, AccountCreator.Status status, String resp) {
                        Log.i(LOG_TAG,
                                "[Account Connection Assistant] onRecoverAccount status is "
                                        + status);
                        if (status.equals(AccountCreator.Status.RequestOk)) {
                            Intent intent =
                                    new Intent(
                                            AccountConnectionAssistantActivity.this,
                                            PhoneAccountValidationAssistantActivity.class);
                            intent.putExtra("isLoginVerification", true);
                            startActivity(intent);
                        } else {
                            btnConnect.setEnabled(true);
                            showGenericErrorDialog(status);
                        }
                    }
                };
    }

    @Override
    protected void onResume() {
        super.onResume();

        Core core = LinphoneManager.getCore(this);
        if (core != null) {
            reloadLinphoneAccountCreatorConfig();
        }

        AccountCreator accountCreator = getAccountCreator();
        if (accountCreator != null) {
            accountCreator.addListener(mListener);
        }

        String phoneNumber = getDevicePhoneNumber();
        if (phoneNumber != null) {
            mPhoneNumber.setText(phoneNumber);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AccountCreator accountCreator = getAccountCreator();
        if (accountCreator != null) {
            accountCreator.removeListener(mListener);
        }
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.assistant_btn_login:
                AccountCreator accountCreator = getAccountCreator();
                accountCreator.reset();
                view.setEnabled(false);

                accountCreator.setPhoneNumber(
                        mPhoneNumber.getText().toString(),
                        mPrefix.getText().toString());
                accountCreator.setUsername(accountCreator.getPhoneNumber());

                AccountCreator.Status status = accountCreator.recoverAccount();
                if (status != AccountCreator.Status.RequestOk) {
                    Log.e(LOG_TAG,
                            "[Account Connection Assistant] recoverAccount returned "
                                    + status);
                    view.setEnabled(true);
                    showGenericErrorDialog(status);
                }
                break;
            default:
                break;
        }
    }

    private void updateConnectButtonAndDisplayError() {
        if (mPrefix.getText().toString().isEmpty() || mPhoneNumber.getText().toString().isEmpty())
            return;

        int status = arePhoneNumberAndPrefixOk(mPrefix, mPhoneNumber);
        if (status == AccountCreator.PhoneNumberStatus.Ok.toInt()) {
            btnConnect.setEnabled(true);
            mError.setText("");
            mError.setVisibility(View.INVISIBLE);
        } else {
            btnConnect.setEnabled(false);
            mError.setText(getErrorFromPhoneNumberStatus(status));
            mError.setVisibility(View.VISIBLE);
        }
    }
}
