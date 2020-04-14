package com.sidia.ims.imsphone.assistant;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.EditText;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.activities.MainActivity;
import com.sidia.ims.imsphone.activities.ui.mainactivity.ImsPhoneViewModel;
import com.sidia.ims.imsphone.service.linphone.LinphoneManager;
import com.sidia.ims.imsphone.service.linphone.LinphonePreferences;

import org.linphone.core.AccountCreator;
import org.linphone.core.Core;
import org.linphone.core.ProxyConfig;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class AssistantActivity extends AppCompatActivity {
    private static final String LOG_TAG = "SP-AssistantActivity";

    public AccountCreator getAccountCreator() {
        return LinphoneManager.getInstance(this).getAccountCreator();
    }

    protected void showAccountAlreadyExistsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.account_already_exist))
                .setMessage(getString(R.string.assistant_phone_number_unavailable))
                .show();
    }

    protected void showGenericErrorDialog(AccountCreator.Status status) {
        String message;

        switch (status) {
            // TODO handle other possible status
            case PhoneNumberInvalid:
                message = getString(R.string.phone_number_invalid);
                break;
            case WrongActivationCode:
                message = getString(R.string.activation_code_invalid);
                break;
            case PhoneNumberOverused:
                message = getString(R.string.phone_number_overuse);
                break;
            case AccountNotExist:
                message = getString(R.string.account_doesnt_exist);
                break;
            default:
                message = getString(R.string.error_unknown);
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error))
                .setMessage(message)
                .show();
    }

    protected  int arePhoneNumberAndPrefixOk(EditText prefixEditText, EditText phoneNumberEditText) {
        String prefix = prefixEditText.getText().toString();
        if (prefix.startsWith("+")) {
            prefix = prefix.substring(1);
        }

        String phoneNumber = phoneNumberEditText.getText().toString();
        AccountCreator accountCreator = getAccountCreator();

        int result = 4;
        if (accountCreator != null) {
            result = accountCreator.setPhoneNumber(phoneNumber, prefix);
        }
        return result;
    }

    String getErrorFromPhoneNumberStatus(int status) {
        AccountCreator.PhoneNumberStatus phoneNumberStatus =
                AccountCreator.PhoneNumberStatus.fromInt(status);
        switch (phoneNumberStatus) {
            case InvalidCountryCode:
                return getString(R.string.country_code_invalid);
            case TooShort:
                return getString(R.string.phone_number_too_short);
            case TooLong:
                return getString(R.string.phone_number_too_long);
            case Invalid:
                return getString(R.string.phone_number_invalid);
        }
        return null;
    }

    void createProxyConfigAndLeaveAssistant() {
        createProxyConfigAndLeaveAssistant(false);
    }

    void createProxyConfigAndLeaveAssistant(boolean isGenericAccount) {
        Core core = LinphoneManager.getCore(this);
        boolean useLinphoneDefaultValues =
                getString(R.string.default_domain).equals(getAccountCreator().getDomain());

        if (isGenericAccount) {
            if (useLinphoneDefaultValues) {
                Log.i(LOG_TAG,
                        "[Assistant] Default domain found for generic connection, reloading configuration");
                core.loadConfigFromXml(
                        LinphonePreferences.instance().getLinphoneDynamicConfigFile());
            } else {
                Log.i(LOG_TAG, "[Assistant] Third party domain found, keeping default values");
            }
        }

        ProxyConfig proxyConfig = getAccountCreator().createProxyConfig();

        if (isGenericAccount) {
            if (useLinphoneDefaultValues) {
                // Restore default values
                Log.i(LOG_TAG, "[Assistant] Restoring default assistant configuration");
                core.loadConfigFromXml(
                        LinphonePreferences.instance().getDefaultDynamicConfigFile());
            } else {
                // If this isn't a sip.linphone.org account, disable push notifications and enable
                // service notification, otherwise incoming calls won't work (most probably)
                if (proxyConfig != null) {
                    proxyConfig.setPushNotificationAllowed(false);
                }
                Log.w(LOG_TAG,
                        "[Assistant] Unknown domain used, push probably won't work, enable service mode");
                LinphonePreferences.instance().setServiceNotificationVisibility(true);
            }
        }

        if (proxyConfig != null) {
            LinphonePreferences.instance().firstLaunchSuccessful();
            goToDialerActivity();
        }
    }

    void goToDialerActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        startActivity(intent);
    }

    protected String getDevicePhoneNumber() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ImsPhoneViewModel.checkPermissions(this, ImsPhoneViewModel.PERMISSIONS);
                return null;
            }
            return tm.getLine1Number();
        } catch (Exception e) {
            Log.e(LOG_TAG,"[Assistant] " + e);
        }
        return null;
    }

    protected void reloadLinphoneAccountCreatorConfig() {
        Log.i(LOG_TAG, "[Assistant] Reloading configuration with specifics");
        reloadAccountCreatorConfig(LinphonePreferences.instance().getLinphoneDynamicConfigFile());
    }

    private void reloadAccountCreatorConfig(String path) {
        Core core = LinphoneManager.getCore(this);
        if (core != null) {
            core.loadConfigFromXml(path);
            AccountCreator accountCreator = getAccountCreator();
            accountCreator.reset();
            accountCreator.setLanguage(Locale.getDefault().getLanguage());
        }
    }
}
