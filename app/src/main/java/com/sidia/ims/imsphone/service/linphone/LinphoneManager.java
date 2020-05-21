package com.sidia.ims.imsphone.service.linphone;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.assistant.PhoneAccountLinkingAssistantActivity;
import com.sidia.ims.imsphone.call.linphone.CallManager;
import com.sidia.ims.imsphone.utils.ImsPhoneUtils;
import com.sidia.ims.imsphone.utils.linphone.LinphoneUtils;

import org.linphone.core.AccountCreator;
import org.linphone.core.AccountCreatorListenerStub;
import org.linphone.core.BuildConfig;
import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListener;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Factory;
import org.linphone.core.PresenceBasicStatus;
import org.linphone.core.PresenceModel;
import org.linphone.core.ProxyConfig;
import org.linphone.core.Reason;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class LinphoneManager {
    private static String LOG_TAG = "SP-LinphoneManager";
    private final String mBasePath;
    private final String mCallLogDatabaseFile;
    private final String mUserCertsPath;

    private CallManager mCallManager;
    private final LinphonePreferences mPrefs;

    private final Context mContext;
    private boolean mExited;
    private Core mCore;
    private CoreListenerStub mCoreListener;
    private boolean mCallGsmON;
    private AccountCreator mAccountCreator;
    private AccountCreatorListenerStub mAccountCreatorListener;
    private Runnable mIterateRunnable;
    private Timer mTimer;



    public LinphoneManager(Context context) {
        mContext = context;
        mBasePath = context.getFilesDir().getAbsolutePath();
        mCallLogDatabaseFile = mBasePath + "/linphone-log-history.db";
        mUserCertsPath = mBasePath + "/user-certs";
        mExited = false;

        mPrefs = LinphonePreferences.instance();

        mCallManager = new CallManager(context);
        mCoreListener =
                new CoreListenerStub() {
                    @SuppressLint("Wakelock")
                    @Override
                    public void onCallStateChanged(
                            final Core core,
                            final Call call,
                            final Call.State state,
                            final String message) {
                        Log.i(LOG_TAG, "[Manager] Call state is [" + state + "]");
                        if (state == Call.State.IncomingReceived
                                && !call.equals(core.getCurrentCall())) {
                            if (call.getReplacedCall() != null) {
                                // attended transfer will be accepted automatically.
                                return;
                            }
                        }

                        if ((state == Call.State.IncomingReceived || state == Call.State.IncomingEarlyMedia)
                                && getCallGsmON()) {
                            if (mCore != null) {
                                call.decline(Reason.Busy);
                            }
                        } else if (state == Call.State.IncomingReceived
                                && (LinphonePreferences.instance().isAutoAnswerEnabled())
                                && !getCallGsmON()) {
                            TimerTask lTask =
                                    new TimerTask() {
                                        @Override
                                        public void run() {
                                            if (mCore != null) {
                                                if (mCore.getCallsNb() > 0) {
                                                   mCallManager.acceptCall(call);
//FIXME                                                    mAudioManager.routeAudioToEarPiece();
                                                }
                                            }
                                        }
                                    };
//FIXME                            mAutoAnswerTimer = new Timer("Auto answer");
//FIXME                            mAutoAnswerTimer.schedule(lTask, mPrefs.getAutoAnswerTime());
                        } else if (state == Call.State.End || state == Call.State.Error) {
                            if (mCore.getCallsNb() == 0) {
                                // Disabling proximity sensor
//FIXME                                enableProximitySensing(false);
                            }
                        }
                    }
                };

        mAccountCreatorListener =
                new AccountCreatorListenerStub() {
                    @Override
                    public void onIsAccountExist(
                            AccountCreator accountCreator,
                            AccountCreator.Status status,
                            String resp) {
                        if (status.equals(AccountCreator.Status.AccountExist)) {
                            accountCreator.isAccountLinked();
                        }
                    }

                    @Override
                    public void onLinkAccount(
                            AccountCreator accountCreator,
                            AccountCreator.Status status,
                            String resp) {
                        if (status.equals(AccountCreator.Status.AccountNotLinked)) {
                            askLinkWithPhoneNumber();
                        }
                    }

                    @Override
                    public void onIsAccountLinked(
                            AccountCreator accountCreator,
                            AccountCreator.Status status,
                            String resp) {
                        if (status.equals(AccountCreator.Status.AccountNotLinked)) {
                            askLinkWithPhoneNumber();
                        }
                    }
                };
    }

    public static synchronized LinphoneManager getInstance(Context context) {
        LinphoneManager manager = LinphoneContext.instance(context).getLinphoneManager();
        if (manager == null) {
            throw new RuntimeException(
                    "[Manager] Linphone Manager should be created before accessed");
        }
        if (manager.mExited) {
            throw new RuntimeException(
                    "[Manager] Linphone Manager was already destroyed. "
                            + "Better use getCore and check returned value");
        }
        return manager;
    }

    public static synchronized Core getCore(Context context) {
        if (!LinphoneContext.isReady()) return null;

        if (getInstance(context).mExited) {
            // Can occur if the UI thread play a posted event but in the meantime the
            // LinphoneManager was destroyed
            // Ex: stop call and quickly terminate application.
            return null;
        }
        return getInstance(context).mCore;
    }

    /* Account linking */

    public AccountCreator getAccountCreator() {
        if (mAccountCreator == null) {
            if (mCore != null) {
                Log.d(LOG_TAG, "[Manager] Account creator shouldn't be null !");
                mAccountCreator =
                        mCore.createAccountCreator(LinphonePreferences.instance().getXmlrpcUrl());
                mAccountCreator.setListener(mAccountCreatorListener);
            }
        }
        return mAccountCreator;
    }

    public void isAccountWithAlias() {
        if (mCore.getDefaultProxyConfig() != null) {
            long now = new Timestamp(new Date().getTime()).getTime();
            AccountCreator accountCreator = getAccountCreator();
            if (LinphonePreferences.instance().getLinkPopupTime() == null
                    || Long.parseLong(LinphonePreferences.instance().getLinkPopupTime()) < now) {
                accountCreator.reset();
                accountCreator.setUsername(
                        LinphonePreferences.instance()
                                .getAccountUsername(
                                        LinphonePreferences.instance().getDefaultAccountIndex()));
                accountCreator.isAccountExist();
            }
        } else {
            LinphonePreferences.instance().setLinkPopupTime(null);
        }
    }


    public synchronized void startLibLinphone(boolean isPush, CoreListener listener) {
        try {
            mCore =
                    Factory.instance()
                            .createCore(
                                    mPrefs.getLinphoneDefaultConfig(),
                                    mPrefs.getLinphoneFactoryConfig(),
                                    mContext);
            mCore.addListener(listener);
            mCore.addListener(mCoreListener);

            if (isPush) {
                Log.w(LOG_TAG,
                        "[Manager] We are here because of a received push notification, enter background mode before starting the Core");
                mCore.enterBackground();
            }

            mCore.start();

            mIterateRunnable =
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mCore != null) {
                                mCore.iterate();
                            }
                        }
                    };
            TimerTask lTask =
                    new TimerTask() {
                        @Override
                        public void run() {
                            LinphoneUtils.dispatchOnUIThread(mIterateRunnable);
                        }
                    };
            /*use schedule instead of scheduleAtFixedRate to avoid iterate from being call in burst after cpu wake up*/
            mTimer = new Timer("Linphone scheduler");
            mTimer.schedule(lTask, 0, 20);

            configureCore();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage() + "[Manager] Cannot start linphone");
        }
    }

    public static synchronized CallManager getCallManager(Context context) {
        return getInstance(context).mCallManager;
    }

    public void doVoIPCall(String phoneNumber) {
        if (phoneNumber.length() > 0) {
            LinphoneManager.getCallManager(mContext).newOutgoingCall(phoneNumber);
        }
    }

    public synchronized void destroy() {
        destroyManager();
        // Wait for Manager to destroy everything before setting mExited to true
        // Otherwise some objects might crash during their own destroy if they try to call
        // LinphoneManager.getCore(), for example to unregister a listener
        mExited = true;
    }

    public void restartCore() {
        Log.w(LOG_TAG,"[Manager] Restarting Core");
        mCore.stop();
        mCore.start();
    }

    public boolean getCallGsmON() {
        return mCallGsmON;
    }

    public void setCallGsmON(boolean on) {
        mCallGsmON = on;
        if (on && mCore != null) {
            mCore.pauseAllCalls();
        }
    }

    private void askLinkWithPhoneNumber() {
        if (!LinphonePreferences.instance().isLinkPopupEnabled()) return;

        long now = new Timestamp(new Date().getTime()).getTime();
        if (LinphonePreferences.instance().getLinkPopupTime() != null
                && Long.parseLong(LinphonePreferences.instance().getLinkPopupTime()) >= now) return;

        ProxyConfig proxyConfig = mCore.getDefaultProxyConfig();
        if (proxyConfig == null) return;
        if (!proxyConfig.getDomain().equals(ImsPhoneUtils.getString(mContext, R.string.default_domain))) return;

        long future = new Timestamp(mContext.getResources().getInteger(
                R.integer.phone_number_linking_popup_time_interval))
                .getTime();
        long newDate = now + future;

        LinphonePreferences.instance().setLinkPopupTime(String.valueOf(newDate));

        final Dialog dialog = ImsPhoneUtils.getDialog(mContext, String.format(
                ImsPhoneUtils.getString(mContext, R.string.link_account_popup),
                proxyConfig.getIdentityAddress().asStringUriOnly()));

        Button btnOk = dialog.findViewById(R.id.dialog_ok_button);
        btnOk.setText(ImsPhoneUtils.getString(mContext, R.string.link));
        btnOk.setVisibility(View.VISIBLE);

        Button btnCancel = dialog.findViewById(R.id.dialog_cancel_button);
        btnCancel.setText(ImsPhoneUtils.getString(mContext, R.string.maybe_later));

        final CheckBox doNotAskAgain = dialog.findViewById(R.id.doNotAskAgain);
        doNotAskAgain.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doNotAskAgain.setChecked(!doNotAskAgain.isChecked());
                    }
                });

        btnOk.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent assistant = new Intent();
                        assistant.setClass(mContext, PhoneAccountLinkingAssistantActivity.class);
                        mContext.startActivity(assistant);
                        dialog.dismiss();
                    }
                });

        btnCancel.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (doNotAskAgain.isChecked()) {
                            LinphonePreferences.instance().enableLinkPopup(false);
                        }
                        dialog.dismiss();
                    }
                });
        dialog.show();
    }

    private synchronized void configureCore() {
        Log.i(LOG_TAG,"[Manager] Configuring Core");

        String deviceName = mPrefs.getDeviceName(mContext);
        String appName = mContext.getResources().getString(R.string.user_agent);
        String androidVersion = BuildConfig.VERSION_NAME;
        String userAgent = appName + "/" + androidVersion + " (" + deviceName + ") LinphoneSDK";

        mCore.setZrtpSecretsFile(mBasePath + "/zrtp_secrets");
        mCore.setUserAgent(
                userAgent,
                ImsPhoneUtils.getString(mContext, R.string.linphone_sdk_version)
                        + " ("
                        + ImsPhoneUtils.getString(mContext, R.string.linphone_sdk_branch)
                        + ")");

        mCore.setCallLogsDatabasePath(mCallLogDatabaseFile);
        mCore.setUserCertificatesPath(mUserCertsPath);
//FIXME        enableDeviceRingtone(mPrefs.isDeviceRingtoneEnabled());

        int availableCores = Runtime.getRuntime().availableProcessors();
        Log.w(LOG_TAG,"[Manager] MediaStreamer : " + availableCores + " cores detected and configured");

        mCore.migrateLogsFromRcToDb();

        mAccountCreator = mCore.createAccountCreator(LinphonePreferences.instance().getXmlrpcUrl());
        mAccountCreator.setListener(mAccountCreatorListener);
        mCallGsmON = false;

        Log.i(LOG_TAG,"[Manager] Core configured");
    }

    private synchronized void destroyManager() {
        Log.w(LOG_TAG,"[Manager] Destroying Manager");
        changeStatusToOffline();

        if (mCallManager != null) mCallManager.destroy();
        if (mTimer != null) mTimer.cancel();

        if (mCore != null) {
            destroyCore();
            mCore = null;
        }
    }

    private void destroyCore() {
        Log.w(LOG_TAG,"[Manager] Destroying Core");
        if (LinphonePreferences.instance() != null) {
            // We set network reachable at false before destroying the Core
            // to not send a register with expires at 0
            if (LinphonePreferences.instance().isPushNotificationEnabled()) {
                Log.w(LOG_TAG,
                        "[Manager] Setting network reachability to False to prevent unregister and allow incoming push notifications");
                mCore.setNetworkReachable(false);
            }
        }
        mCore.stop();
        mCore.removeListener(mCoreListener);
    }

    private void changeStatusToOffline() {
        if (mCore != null) {
            PresenceModel model = mCore.getPresenceModel();
            model.setBasicStatus(PresenceBasicStatus.Closed);
            mCore.setPresenceModel(model);
        }
    }
}
