package com.sidia.ims.imsphone.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.view.Menu;
import android.view.View;

import com.google.android.material.navigation.NavigationView;
import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.assistant.MenuAssistantActivity;
import com.sidia.ims.imsphone.service.linphone.LinphoneContext;
import com.sidia.ims.imsphone.service.linphone.LinphoneManager;
import com.sidia.ims.imsphone.service.linphone.LinphoneService;
import com.sidia.ims.imsphone.service.linphone.ServiceWaitThread;
import com.sidia.ims.imsphone.service.linphone.LinphonePreferences;
import com.sidia.ims.imsphone.utils.ImsPhoneUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ServiceWaitThread.Listener {
    private static final int REQUEST_ID = 1;

    private AppBarConfiguration mAppBarConfiguration;
    private NavController mNavController;

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

        requestRole();
        ImsPhoneUtils.ensureServiceIsRunning(this);
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

    private void requestRole() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            @SuppressLint("WrongConstant")
            RoleManager roleManager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);

            startActivityForResult(intent, REQUEST_ID);
        } else {
            TelecomManager tm = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            if (tm.getDefaultDialerPackage() != getPackageName()) {
                Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                        .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, getPackageName());
                startActivity(intent);
            }
        }
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

    private void startLinphoneService() {
        Intent intent = new Intent().setClass(this, LinphoneService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        new ServiceWaitThread(this).start();
    }
}
