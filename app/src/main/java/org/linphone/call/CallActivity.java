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
package org.linphone.call;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import org.linphone.LinphoneContext;
import org.linphone.LinphoneManager;
import org.linphone.R;
import org.linphone.activities.LinphoneGenericActivity;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListener;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.tools.Log;
import org.linphone.service.LinphoneService;
import org.linphone.settings.LinphonePreferences;
import org.linphone.utils.LinphoneUtils;

public class CallActivity extends LinphoneGenericActivity
        implements CallStatusBarFragment.StatsClikedListener, CallActivityInterface {
    private static final int SECONDS_BEFORE_HIDING_CONTROLS = 4000;
    private static final int SECONDS_BEFORE_DENYING_CALL_UPDATE = 30000;

    private static final int MIC_TO_DISABLE_MUTE = 1;
    private static final int ALL_PERMISSIONS = 4;

    private static class HideControlsRunnable implements Runnable {
        private WeakReference<CallActivity> mWeakCallActivity;

        public HideControlsRunnable(CallActivity activity) {
            mWeakCallActivity = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            // Make sure that at the time this is executed this is still required
            if (!LinphoneContext.isReady()) return;

            Call call = LinphoneManager.getCore().getCurrentCall();
            if (call != null && call.getCurrentParams().videoEnabled()) {
                CallActivity activity = mWeakCallActivity.get();
                if (activity != null) activity.updateButtonsVisibility(false);
            }
        }
    }

    private final HideControlsRunnable mHideControlsRunnable = new HideControlsRunnable(this);

    private RelativeLayout mButtons, mActiveCalls, mActiveCallHeader;
    private LinearLayout mCallsList;
    private ImageView mMicro, mSpeaker;
    private ImageView mPause;
    private ImageView mExtrasButtons;
    private ImageView mAudioRoute, mRouteEarpiece, mRouteSpeaker;
    private Chronometer mCallTimer;
    private CountDownTimer mCallUpdateCountDownTimer;
    private Dialog mCallUpdateDialog;
    private TextView mContactName;

    private CallStatsFragment mStatsFragment;
    private Core mCore;
    private CoreListener mListener;
    private AndroidAudioManager mAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Compatibility.setShowWhenLocked(this, true);

        setContentView(R.layout.call);

        mActiveCalls = findViewById(R.id.active_calls);
        mActiveCallHeader = findViewById(R.id.active_call);
        mCallsList = findViewById(R.id.calls_list);
        mButtons = findViewById(R.id.buttons);

        mCallTimer = findViewById(R.id.current_call_timer);
        mMicro = findViewById(R.id.micro);
        mMicro.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (checkAndRequestPermission(
                                Manifest.permission.RECORD_AUDIO, MIC_TO_DISABLE_MUTE)) {
                            toggleMic();
                        }
                    }
                });

        mSpeaker = findViewById(R.id.speaker);
        mSpeaker.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleSpeaker();
                    }
                });

        mAudioRoute = findViewById(R.id.audio_route);
        mAudioRoute.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleAudioRouteButtons();
                    }
                });

        mRouteEarpiece = findViewById(R.id.route_earpiece);
        mRouteEarpiece.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAudioManager.routeAudioToEarPiece();
                        updateAudioRouteButtons();
                    }
                });

        mRouteSpeaker = findViewById(R.id.route_speaker);
        mRouteSpeaker.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAudioManager.routeAudioToSpeaker();
                        updateAudioRouteButtons();
                    }
                });

        mExtrasButtons = findViewById(R.id.options);
        mExtrasButtons.setSelected(false);
        mExtrasButtons.setEnabled(!getResources().getBoolean(R.bool.disable_options_in_call));

        ImageView numpadButton = findViewById(R.id.dialer);
        numpadButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View numpad = findViewById(R.id.numpad);
                        boolean isNumpadVisible = numpad.getVisibility() == View.VISIBLE;
                        numpad.setVisibility(isNumpadVisible ? View.GONE : View.VISIBLE);
                        v.setSelected(!isNumpadVisible);
                    }
                });
        numpadButton.setSelected(false);

        ImageView hangUp = findViewById(R.id.hang_up);
        hangUp.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinphoneManager.getCallManager().terminateCurrentCallOrConferenceOrAll();
                    }
                });

        mPause = findViewById(R.id.pause);
        mPause.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        togglePause(mCore.getCurrentCall());
                    }
                });

        DrawerLayout sideMenu = findViewById(R.id.side_menu);
        RelativeLayout sideMenuContent = findViewById(R.id.side_menu_content);
        mStatsFragment =
                (CallStatsFragment) getFragmentManager().findFragmentById(R.id.call_stats_fragment);
        mStatsFragment.setDrawer(sideMenu, sideMenuContent);

        CallStatusBarFragment statusBarFragment =
                (CallStatusBarFragment)
                        getFragmentManager().findFragmentById(R.id.status_bar_fragment);
        statusBarFragment.setStatsListener(this);

        mListener =
                new CoreListenerStub() {

                    @Override
                    public void onCallStateChanged(
                            Core core, Call call, Call.State state, String message) {
                        if (state == Call.State.End || state == Call.State.Released) {
                            if (core.getCallsNb() == 0) {
                                finish();
                            }
                        } else if (state == Call.State.StreamsRunning) {
                            setCurrentCallContactInformation();
                        } else if (state == Call.State.UpdatedByRemote) {
                            // If the correspondent asks for video while in audio call
                            boolean videoEnabled = LinphonePreferences.instance().isVideoEnabled();
                            if (!videoEnabled) {
                                // Video is disabled globally, don't even ask user
                                acceptCallUpdate(false);
                                return;
                            }

                            boolean showAcceptUpdateDialog =
                                    LinphoneManager.getCallManager()
                                            .shouldShowAcceptCallUpdateDialog(call);
                            if (showAcceptUpdateDialog) {
                                createTimerForDialog(SECONDS_BEFORE_DENYING_CALL_UPDATE);
                            }
                        }

                        updateButtons();
                        updateCallsList();
                    }
                };

        mCore = LinphoneManager.getCore();
        if (mCore != null) {
            boolean recordAudioPermissionGranted =
                    checkPermission(Manifest.permission.RECORD_AUDIO);
            if (!recordAudioPermissionGranted) {
                Log.w("[Call Activity] RECORD_AUDIO permission denied, muting microphone");
                mCore.enableMic(false);
            }

            Call call = mCore.getCurrentCall();
            boolean videoEnabled =
                    LinphonePreferences.instance().isVideoEnabled()
                            && call != null
                            && call.getCurrentParams().videoEnabled();

            if (videoEnabled) {
                mAudioManager = LinphoneManager.getAudioManager();
                mAudioManager.routeAudioToSpeaker();
                mSpeaker.setSelected(true);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // This also must be done here in case of an outgoing call accepted
        // before user granted or denied permissions
        // or if an incoming call was answer from the notification
        checkAndRequestCallPermissions();

        mCore = LinphoneManager.getCore();
        if (mCore != null) {
            mCore.addListener(mListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAudioManager = LinphoneManager.getAudioManager();

        updateButtons();
        updateCallsList();
        LinphoneManager.getCallManager().setCallInterface(this);

        if (mCore.getCallsNb() == 0) {
            Log.w("[Call Activity] Resuming but no call found...");
            finish();
        }

        if (LinphoneService.isReady()) LinphoneService.instance().destroyOverlay();
    }

    @Override
    protected void onDestroy() {
        Core core = LinphoneManager.getCore();
        if (core != null) {
            core.removeListener(mListener);
            core.setNativeVideoWindowId(null);
            core.setNativePreviewWindowId(null);
        }

        if (mCallUpdateCountDownTimer != null) {
            mCallUpdateCountDownTimer.cancel();
            mCallUpdateCountDownTimer = null;
        }

        mCallTimer.stop();
        mCallTimer = null;

        mListener = null;
        mStatsFragment = null;

        mButtons = null;
        mActiveCalls = null;
        mActiveCallHeader = null;
        mCallsList = null;
        mMicro = null;
        mSpeaker = null;
        mPause = null;
        mExtrasButtons = null;
        mAudioRoute = null;
        mRouteEarpiece = null;
        mRouteSpeaker = null;
        mContactName = null;
        mCallUpdateDialog = null;

        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAudioManager.onKeyVolumeAdjust(keyCode)) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onStatsClicked() {
        if (mStatsFragment.isOpened()) {
            mStatsFragment.openOrCloseSideMenu(false, true);
        } else {
            mStatsFragment.openOrCloseSideMenu(true, true);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Permission not granted, won't change anything

        if (requestCode == ALL_PERMISSIONS) {
            for (int index = 0; index < permissions.length; index++) {
                int granted = grantResults[index];
                if (granted == PackageManager.PERMISSION_GRANTED) {
                    String permission = permissions[index];
                    if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
                        toggleMic();
                    } else if (Manifest.permission.CAMERA.equals(permission)) {
                        LinphoneUtils.reloadVideoDevices();
                    }
                }
            }
        } else {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) return;
            switch (requestCode) {
                case MIC_TO_DISABLE_MUTE:
                    toggleMic();
                    break;
            }
        }
    }

    private boolean checkPermission(String permission) {
        int granted = getPackageManager().checkPermission(permission, getPackageName());
        Log.i(
                "[Permission] "
                        + permission
                        + " permission is "
                        + (granted == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkAndRequestPermission(String permission, int result) {
        if (!checkPermission(permission)) {
            Log.i("[Permission] Asking for " + permission);
            ActivityCompat.requestPermissions(this, new String[] {permission}, result);
            return false;
        }
        return true;
    }

    private void checkAndRequestCallPermissions() {
        ArrayList<String> permissionsList = new ArrayList<>();

        int recordAudio =
                getPackageManager()
                        .checkPermission(Manifest.permission.RECORD_AUDIO, getPackageName());
        Log.i(
                "[Permission] Record audio permission is "
                        + (recordAudio == PackageManager.PERMISSION_GRANTED
                                ? "granted"
                                : "denied"));
        int camera =
                getPackageManager().checkPermission(Manifest.permission.CAMERA, getPackageName());
        Log.i(
                "[Permission] Camera permission is "
                        + (camera == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

        int readPhoneState =
                getPackageManager()
                        .checkPermission(Manifest.permission.READ_PHONE_STATE, getPackageName());
        Log.i(
                "[Permission] Read phone state permission is "
                        + (camera == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

        if (recordAudio != PackageManager.PERMISSION_GRANTED) {
            Log.i("[Permission] Asking for record audio");
            permissionsList.add(Manifest.permission.RECORD_AUDIO);
        }
        if (readPhoneState != PackageManager.PERMISSION_GRANTED) {
            Log.i("[Permission] Asking for read phone state");
            permissionsList.add(Manifest.permission.READ_PHONE_STATE);
        }

        Call call = mCore.getCurrentCall();
        if (LinphonePreferences.instance().shouldInitiateVideoCall()
                || (LinphonePreferences.instance().shouldAutomaticallyAcceptVideoRequests()
                        && call != null
                        && call.getRemoteParams().videoEnabled())) {
            if (camera != PackageManager.PERMISSION_GRANTED) {
                Log.i("[Permission] Asking for camera");
                permissionsList.add(Manifest.permission.CAMERA);
            }
        }

        if (permissionsList.size() > 0) {
            String[] permissions = new String[permissionsList.size()];
            permissions = permissionsList.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, ALL_PERMISSIONS);
        }
    }

    @Override
    public void onUserLeaveHint() {
        if (mCore == null) return;
        Call call = mCore.getCurrentCall();
        if (call == null) return;
        boolean videoEnabled =
                LinphonePreferences.instance().isVideoEnabled()
                        && call.getCurrentParams().videoEnabled();
        if (videoEnabled && getResources().getBoolean(R.bool.allow_pip_while_video_call)) {
            Compatibility.enterPipMode(this);
        }
    }

    @Override
    public void refreshInCallActions() {
        updateButtons();
    }

    @Override
    public void resetCallControlsHidingTimer() {
        LinphoneUtils.removeFromUIThreadDispatcher(mHideControlsRunnable);
        LinphoneUtils.dispatchOnUIThreadAfter(
                mHideControlsRunnable, SECONDS_BEFORE_HIDING_CONTROLS);
    }

    // BUTTONS

    private void updateAudioRouteButtons() {
        mRouteSpeaker.setSelected(mAudioManager.isAudioRoutedToSpeaker());
        mRouteEarpiece.setSelected(mAudioManager.isAudioRoutedToEarpiece());
    }

    private void updateButtons() {
        Call call = mCore.getCurrentCall();
        mMicro.setSelected(!mCore.micEnabled());
        mSpeaker.setSelected(mAudioManager.isAudioRoutedToSpeaker());

        updateAudioRouteButtons();

        mSpeaker.setVisibility(View.VISIBLE);
        mAudioRoute.setVisibility(View.GONE);
        mPause.setEnabled(call != null && !call.mediaInProgress());
    }

    private void toggleMic() {
        mCore.enableMic(!mCore.micEnabled());
        mMicro.setSelected(!mCore.micEnabled());
    }

    private void toggleSpeaker() {
        if (mAudioManager.isAudioRoutedToSpeaker()) {
            mAudioManager.routeAudioToEarPiece();
        } else {
            mAudioManager.routeAudioToSpeaker();
        }
        mSpeaker.setSelected(mAudioManager.isAudioRoutedToSpeaker());
    }

    private void togglePause(Call call) {
        if (call == null) return;

        if (call == mCore.getCurrentCall()) {
            call.pause();
            mPause.setSelected(true);
        } else if (call.getState() == Call.State.Paused) {
            call.resume();
            mPause.setSelected(false);
        }
    }

    private void toggleAudioRouteButtons() {
        mAudioRoute.setSelected(!mAudioRoute.isSelected());
        mRouteEarpiece.setVisibility(mAudioRoute.isSelected() ? View.VISIBLE : View.GONE);
        mRouteSpeaker.setVisibility(mAudioRoute.isSelected() ? View.VISIBLE : View.GONE);
    }

    private void updateButtonsVisibility(boolean visible) {
        findViewById(R.id.status_bar_fragment).setVisibility(visible ? View.VISIBLE : View.GONE);
        if (mActiveCalls != null) mActiveCalls.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (mButtons != null) mButtons.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // CALL UPDATE

    private void createTimerForDialog(long time) {
        mCallUpdateCountDownTimer =
                new CountDownTimer(time, 1000) {
                    public void onTick(long millisUntilFinished) {}

                    public void onFinish() {
                        if (mCallUpdateDialog != null) {
                            mCallUpdateDialog.dismiss();
                            mCallUpdateDialog = null;
                        }
                        acceptCallUpdate(false);
                    }
                }.start();
    }

    private void acceptCallUpdate(boolean accept) {
        if (mCallUpdateCountDownTimer != null) {
            mCallUpdateCountDownTimer.cancel();
        }
        LinphoneManager.getCallManager().acceptCallUpdate(accept);
    }

    // OTHER

    private void updateCallsList() {
        Call currentCall = mCore.getCurrentCall();
        if (currentCall != null) {
            setCurrentCallContactInformation();
        }

        boolean callThatIsNotCurrentFound = false;
        boolean pausedConferenceDisplayed = false;
        boolean conferenceDisplayed = false;
        mCallsList.removeAllViews();

        for (Call call : mCore.getCalls()) {
            if (call != null && call != currentCall) {
                Call.State state = call.getState();
                if (state == Call.State.Paused
                        || state == Call.State.PausedByRemote
                        || state == Call.State.Pausing) {
                    displayPausedCall(call);
                    callThatIsNotCurrentFound = true;
                }
            }
        }

        mCallsList.setVisibility(
                pausedConferenceDisplayed || callThatIsNotCurrentFound ? View.VISIBLE : View.GONE);
        mActiveCallHeader.setVisibility(
                currentCall != null && !conferenceDisplayed ? View.VISIBLE : View.GONE);
    }

    private void displayPausedCall(final Call call) {
        LinearLayout callView =
                (LinearLayout)
                        LayoutInflater.from(this).inflate(R.layout.call_inactive_row, null, false);

        TextView contactName = callView.findViewById(R.id.contact_name);
        Address address = call.getRemoteAddress();
        String displayName = LinphoneUtils.getAddressDisplayName(address);
        contactName.setText(displayName);

        Chronometer timer = callView.findViewById(R.id.call_timer);
        timer.setBase(SystemClock.elapsedRealtime() - 1000 * call.getDuration());
        timer.start();

        ImageView resumeCall = callView.findViewById(R.id.call_pause);
        resumeCall.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        togglePause(call);
                    }
                });

        mCallsList.addView(callView);
    }

    private void updateCurrentCallTimer() {
        Call call = mCore.getCurrentCall();
        if (call == null) return;

        mCallTimer.setBase(SystemClock.elapsedRealtime() - 1000 * call.getDuration());
        mCallTimer.start();
    }

    private void setCurrentCallContactInformation() {
        updateCurrentCallTimer();
        Call call = mCore.getCurrentCall();
        if (call == null) return;

        String displayName = LinphoneUtils.getAddressDisplayName(call.getRemoteAddress());
        mContactName.setText(displayName);
    }
}
