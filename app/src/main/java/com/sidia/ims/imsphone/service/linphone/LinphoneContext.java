package com.sidia.ims.imsphone.service.linphone;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.call.CallIncomingActivity;
import com.sidia.ims.imsphone.call.CallOutgoingActivity;
import com.sidia.ims.imsphone.notifications.NotificationsManager;

import org.linphone.core.Call;
import org.linphone.core.ConfiguringState;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.GlobalState;
import org.linphone.mediastream.Version;

import java.util.ArrayList;

import static android.content.Intent.ACTION_MAIN;

public class LinphoneContext {
    private static final String LOG_TAG = "SP-LinphoneContext";
    private static final int NOTIFICATION_ID = 38;
    private static LinphoneContext sInstance = null;

    private Context mContext;
    private CoreListenerStub mListener;
    private NotificationsManager mNotificationManager;
    private LinphoneManager mLinphoneManager;
    private final ArrayList<CoreStartedListener> mCoreStartedListeners;

    public LinphoneContext(Context context) {
        mContext = context;
        mCoreStartedListeners = new ArrayList<>();
        LinphonePreferences.instance().setContext(context);

        Log.i(LOG_TAG, "[Context] Ready");
        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onGlobalStateChanged(Core core, GlobalState state, String message) {
                        Log.i(LOG_TAG, "[Context] Global state is [" + state + "]");

                        if (state == GlobalState.On) {
                            for (CoreStartedListener listener : mCoreStartedListeners) {
                                listener.onCoreStarted();
                            }
                        }
                    }

                    @Override
                    public void onConfiguringStatus(
                            Core core, ConfiguringState status, String message) {
                        Log.i(LOG_TAG, "[Context] Configuring state is [" + status + "]");

                        if (status == ConfiguringState.Successful) {
                            LinphonePreferences.instance().setPushNotificationEnabled(
                                    LinphonePreferences.instance().isPushNotificationEnabled());
                        }
                    }

                    @Override
                    public void onCallStateChanged(
                            Core core, Call call, Call.State state, String message) {
                        Log.i(LOG_TAG, "[Context] Call state is [" + state + "]");

                        if (mContext.getResources().getBoolean(R.bool.enable_call_notification)) {
                            mNotificationManager.displayCallNotification(mContext, NOTIFICATION_ID,
                                    call.getRemoteAddressAsString());
                        }

                        if (state == Call.State.IncomingReceived
                                || state == Call.State.IncomingEarlyMedia) {
                            // Starting SDK 24 (Android 7.0) we rely on the fullscreen intent of the
                            // call incoming notification
                            if (Version.sdkStrictlyBelow(Version.API24_NOUGAT_70)) {
                                if (!mLinphoneManager.getCallGsmON()) onIncomingReceived();
                            }

                            // In case of push notification Service won't be started until here
                            if (!LinphoneService.isReady()) {
                                Log.i(LOG_TAG, "[Context] Service not running, starting it");
                                Intent intent = new Intent(ACTION_MAIN);
                                intent.setClass(mContext, LinphoneService.class);
                                mContext.startService(intent);
                            }
                        } else if (state == Call.State.OutgoingInit) {
                            onOutgoingStarted();
                        } else if (state == Call.State.Connected) {
                            onCallStarted();
                        } else if (state == Call.State.End
                                || state == Call.State.Released
                                || state == Call.State.Error) {
                            if (state == Call.State.Released
                                    && call.getCallLog().getStatus() == Call.Status.Missed) {
                                mNotificationManager.displayMissedCallNotification(call);
                            }
                        }
                    }
                };

        mLinphoneManager = new LinphoneManager(context);
        mNotificationManager = new NotificationsManager(context);
    }

    public static boolean isReady() {
        return sInstance != null;
    }

    public static LinphoneContext instance(Context context) {
        if (sInstance == null) {
            sInstance = new LinphoneContext(context);
        }
        return sInstance;
    }

    public Context getApplicationContext() {
        return mContext;
    }

    public LinphoneManager getLinphoneManager() {
        return mLinphoneManager;
    }

    public void start(boolean isPush) {
        Log.i(LOG_TAG, "[Context] Starting, push status is " + isPush);
        mLinphoneManager.startLibLinphone(isPush, mListener);
    }

    public void addCoreStartedListener(CoreStartedListener listener) {
        mCoreStartedListeners.add(listener);
    }

    public void removeCoreStartedListener(CoreStartedListener listener) {
        mCoreStartedListeners.remove(listener);
    }

    public void updateContext(Context context) {
        mContext = context;
    }

    public NotificationsManager getNotificationManager() {
        return mNotificationManager;
    }

    /* Call activities */

    private void onIncomingReceived() {
        Intent intent = new Intent(mContext, CallIncomingActivity.class);
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void onOutgoingStarted() {
        Intent intent = new Intent(mContext, CallOutgoingActivity.class);
        // This flag is required to start an Activity from a Service context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void onCallStarted() {
//FIXME        Intent intent = new Intent(mContext, CallActivity.class);
//FIXME        // This flag is required to start an Activity from a Service context
//FIXME        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//FIXME        mContext.startActivity(intent);
    }

    public void destroy() {
        sInstance = null;
    }

    public interface CoreStartedListener {
        void onCoreStarted();
    }
}
