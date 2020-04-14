package com.sidia.ims.imsphone.assistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.activities.ui.mainactivity.ImsPhoneViewModel;
import com.sidia.ims.imsphone.service.linphone.LinphoneManager;
import com.sidia.ims.imsphone.service.linphone.LinphonePreferences;

import org.linphone.core.AccountCreator;
import org.linphone.core.AccountCreatorListenerStub;
import org.linphone.core.Core;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

public class PhoneAccountCreationAssistantActivity extends AssistantActivity implements View.OnClickListener {
    private static final String LOG_TAG = "SP-AccountCreation";

    private Button mCreate;
    private TextView mError;
    private EditText mPrefix, mPhoneNumber;
    private AccountCreatorListenerStub mListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_phone_account_creation);

        mError = findViewById(R.id.phone_number_error);

        mCreate = findViewById(R.id.assistant_create);
        mCreate.setEnabled(false);

        mPrefix = findViewById(R.id.dial_code);
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
                        updateCreateButtonAndDisplayError();
                    }
                });

        mListener = new AccountCreatorListenerStub() {
            public void onIsAccountExist(
                    AccountCreator creator, AccountCreator.Status status, String resp) {
                Log.i(LOG_TAG,
                        "[Phone Account Creation Assistant] onIsAccountExist status is "
                                + status);
                if (status.equals(AccountCreator.Status.AccountExist)
                        || status.equals(AccountCreator.Status.AccountExistWithAlias)) {
                    showAccountAlreadyExistsDialog();
                    enableButtonsAndFields(true);
                } else if (status.equals(AccountCreator.Status.AccountNotExist)) {
                    status = getAccountCreator().createAccount();
                    if (status != AccountCreator.Status.RequestOk) {
                        Log.e(LOG_TAG,
                                "[Phone Account Creation Assistant] createAccount returned "
                                        + status);
                        enableButtonsAndFields(true);
                        showGenericErrorDialog(status);
                    }
                } else {
                    enableButtonsAndFields(true);
                    showGenericErrorDialog(status);
                }
            }

            @Override
            public void onCreateAccount(
                    AccountCreator creator, AccountCreator.Status status, String resp) {
                Log.i(LOG_TAG,
                        "[Phone Account Creation Assistant] onCreateAccount status is "
                                + status);
                if (status.equals(AccountCreator.Status.AccountCreated)) {
                    startActivity(new Intent(PhoneAccountCreationAssistantActivity.this,
                            PhoneAccountValidationAssistantActivity.class));
                } else {
                    enableButtonsAndFields(true);
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
        AccountCreator accountCreator = getAccountCreator();
        enableButtonsAndFields(false);

        accountCreator.setUsername(accountCreator.getPhoneNumber());

        AccountCreator.Status status = accountCreator.isAccountExist();
        if (status != AccountCreator.Status.RequestOk) {
            Log.e(LOG_TAG,
                    "[Phone Account Creation Assistant] isAccountExists returned "
                            + status);
            enableButtonsAndFields(true);
            showGenericErrorDialog(status);
        }
    }

    private void enableButtonsAndFields(boolean enable) {
        mPrefix.setEnabled(enable);
        mPhoneNumber.setEnabled(enable);
        mCreate.setEnabled(enable);
    }

    private void updateCreateButtonAndDisplayError() {
        if (mPrefix.getText().toString().isEmpty() || mPhoneNumber.getText().toString().isEmpty())
            return;

        mCreate.setEnabled(true);
        mError.setText("");
        mError.setVisibility(View.INVISIBLE);

        int status = arePhoneNumberAndPrefixOk(mPrefix, mPhoneNumber);
        if (status != AccountCreator.PhoneNumberStatus.Ok.toInt()) {
            mCreate.setEnabled(false);
            mError.setText(getErrorFromPhoneNumberStatus(status));
            mError.setVisibility(View.VISIBLE);
        }
    }
}
