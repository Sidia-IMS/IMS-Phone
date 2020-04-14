package com.sidia.ims.imsphone.assistant;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.service.linphone.LinphonePreferences;

import org.linphone.core.AccountCreator;
import org.linphone.core.AccountCreatorListenerStub;

import androidx.annotation.Nullable;

public class PhoneAccountValidationAssistantActivity extends AssistantActivity implements View.OnClickListener {
    private static final String LOG_TAG = "SP-AccountValidation";

    private Button btnFinishCreation;
    private TextView txtTitle;
    private EditText mSmsCode;

    private int mActivationCodeLength;
    private boolean mIsLinking = false;
    private boolean mIsLogin = false;
    private AccountCreatorListenerStub mListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_phone_account_validation);

        txtTitle = findViewById(R.id.assitant_validation_title);
        if (getIntent() != null && getIntent().getBooleanExtra("isLoginVerification", false)) {
            txtTitle.setText(R.string.assistant_login_linphone);
            mIsLogin = true;
        } else if (getIntent() != null
                && getIntent().getBooleanExtra("isLinkingVerification", false)) {
            txtTitle.setText(R.string.assistant_link_account);
            mIsLinking = true;
        } else {
            txtTitle.setText(R.string.assistant_create_account);
        }

        mActivationCodeLength =
                getResources().getInteger(R.integer.phone_number_validation_code_length);

        TextView phoneNumber = findViewById(R.id.phone_number);
        phoneNumber.setText(getAccountCreator().getPhoneNumber());

        mSmsCode = findViewById(R.id.sms_code);
        mSmsCode.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        btnFinishCreation.setEnabled(s.length() == mActivationCodeLength);
                    }
                });

        btnFinishCreation = findViewById(R.id.finish_account_creation);
        btnFinishCreation.setEnabled(false);
        btnFinishCreation.setOnClickListener(this);

        mListener =
                new AccountCreatorListenerStub() {
                    @Override
                    public void onActivateAccount(
                            AccountCreator creator, AccountCreator.Status status, String resp) {
                        Log.i(LOG_TAG, "[Phone Account Validation] onActivateAccount status is " + status);
                        if (status.equals(AccountCreator.Status.AccountActivated)) {
                            createProxyConfigAndLeaveAssistant();
                        } else {
                            onError(status);
                        }
                    }

                    @Override
                    public void onActivateAlias(
                            AccountCreator creator, AccountCreator.Status status, String resp) {
                        Log.i(LOG_TAG, "[Phone Account Validation] onActivateAlias status is " + status);
                        if (status.equals(AccountCreator.Status.AccountActivated)) {
                            LinphonePreferences.instance().setLinkPopupTime("");
                            goToDialerActivity();
                        } else {
                            onError(status);
                        }
                    }

                    @Override
                    public void onLoginLinphoneAccount(
                            AccountCreator creator, AccountCreator.Status status, String resp) {
                        Log.i(LOG_TAG,
                                "[Phone Account Validation] onLoginLinphoneAccount status is "
                                        + status);
                        if (status.equals(AccountCreator.Status.RequestOk)) {
                            createProxyConfigAndLeaveAssistant();
                        } else {
                            onError(status);
                        }
                    }
                };
    }

    @Override
    protected void onResume() {
        super.onResume();
        getAccountCreator().addListener(mListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getAccountCreator().removeListener(mListener);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.finish_account_creation:
                AccountCreator accountCreator = getAccountCreator();
                btnFinishCreation.setEnabled(false);
                accountCreator.setActivationCode(mSmsCode.getText().toString());

                AccountCreator.Status status;
                if (mIsLinking) {
                    status = accountCreator.activateAlias();
                } else if (mIsLogin) {
                    status = accountCreator.loginLinphoneAccount();
                } else {
                    status = accountCreator.activateAccount();
                }
                if (status != AccountCreator.Status.RequestOk) {
                    Log.e(LOG_TAG,
                            "[Phone Account Validation] "
                                    + (mIsLinking
                                    ? "linkAccount"
                                    : (mIsLogin
                                    ? "loginLinphoneAccount"
                                    : "activateAccount")
                                    + " returned ")
                                    + status);
                    btnFinishCreation.setEnabled(true);
                    showGenericErrorDialog(status);
                }
                break;
            default:
                break;
        }
    }

    private void onError(AccountCreator.Status status) {
        btnFinishCreation.setEnabled(true);
        showGenericErrorDialog(status);
    }
}
