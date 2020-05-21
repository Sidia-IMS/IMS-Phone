package com.sidia.ims.imsphone.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.assistant.MenuAssistantActivity;
import com.sidia.ims.imsphone.menu.SideMenuViewModel;
import com.sidia.ims.imsphone.service.linphone.LinphoneManager;
import com.sidia.ims.imsphone.service.linphone.LinphoneService;
import com.sidia.ims.imsphone.service.linphone.ServiceWaitThread;
import com.sidia.ims.imsphone.service.linphone.LinphonePreferences;
import com.sidia.ims.imsphone.settings.SettingsActivity;
import com.sidia.ims.imsphone.utils.ImsPhoneUtils;

import org.linphone.core.AuthInfo;
import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.ProxyConfig;
import org.linphone.core.RegistrationState;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ServiceWaitThread.Listener {
    private AppBarConfiguration mAppBarConfiguration;
    private NavController mNavController;
    private CoreListenerStub mListener;
    private TextView mMissedCalls;
    private SideMenuViewModel mSideMenuViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        mNavController = Navigation.findNavController(this, R.id.nav_host_fragment);
        mAppBarConfiguration = new AppBarConfiguration.Builder(mNavController.getGraph())
                .setDrawerLayout(drawer).build();
        NavigationUI.setupActionBarWithNavController(this, mNavController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, mNavController);
        View hView =  navigationView.getHeaderView(0);

        ImsPhoneUtils.ensureServiceIsRunning(this);

        mMissedCalls = findViewById(R.id.missed_calls);
        mSideMenuViewModel = new SideMenuViewModel(this, hView);
        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onCallStateChanged(
                            Core core, Call call, Call.State state, String message) {
                        if (state == Call.State.End || state == Call.State.Released) {
                            displayMissedCalls();
                        }
                    }

                    @Override
                    public void onRegistrationStateChanged(
                            Core core,
                            ProxyConfig proxyConfig,
                            RegistrationState state,
                            String message) {
                        mSideMenuViewModel.displayAccountsInSideMenu();

                        if (state == RegistrationState.Ok) {
                            AuthInfo authInfo =
                                    core.findAuthInfo(
                                            proxyConfig.getRealm(),
                                            proxyConfig.getIdentityAddress().getUsername(),
                                            proxyConfig.getDomain());
                            if (authInfo != null
                                    && authInfo.getDomain()
                                    .equals(getString(R.string.default_domain))) {
                                LinphoneManager.getInstance(getApplicationContext()).isAccountWithAlias();
                            }
                        }
                    }
                };
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (LinphoneService.isReady()) {
            onServiceReady();
        } else {
            startLinphoneService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ImsPhoneUtils.ensureServiceIsRunning(this);
        Core core = LinphoneManager.getCore(this);
        if (core != null) {
            core.addListener(mListener);
            displayMissedCalls();
        }
    }

    @Override
    protected void onPause() {
        Core core = LinphoneManager.getCore(this);
        if (core != null) {
            core.removeListener(mListener);
        }

        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.history:
                mNavController.navigate(R.id.nav_history);
                break;
            case R.id.dialer:
                mNavController.navigate(R.id.nav_home);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(mNavController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onServiceReady() {
        if (LinphonePreferences.instance().isFirstLaunch()) {
            Intent intent = new Intent();
            intent.setClass(this, MenuAssistantActivity.class);
            if (getIntent() != null && getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }
            intent.setAction(getIntent().getAction());
            intent.setType(getIntent().getType());
            intent.setData(getIntent().getData());
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        mMissedCalls = null;
        mListener = null;

        super.onDestroy();
    }

    public void showAccountSettings(int accountIndex) {
        Intent intent = new Intent(this, SettingsActivity.class);
        addFlagsToIntent(intent);
        intent.putExtra("Account", accountIndex);
        startActivity(intent);
    }

    protected void displayMissedCalls() {
        int count = 0;
        Core core = LinphoneManager.getCore(this);
        if (core != null) {
            count = core.getMissedCallsCount();
        }

        if (count > 0) {
            mMissedCalls.setText(String.valueOf(count));
            mMissedCalls.setVisibility(View.VISIBLE);
        } else {
            mMissedCalls.clearAnimation();
            mMissedCalls.setVisibility(View.GONE);
        }
    }

    private void startLinphoneService() {
        Intent intent = new Intent().setClass(this, LinphoneService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        new ServiceWaitThread(this).start();
    }

    private void addFlagsToIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }
}
