package com.sidia.ims.imsphone.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.view.View;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.dialer.DialerFragment;
import com.sidia.ims.imsphone.history.HistoryFragment;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_ID = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            DialerFragment dialerFragment = DialerFragment.newInstance();
            FragmentTransaction transaction = getTransaction(dialerFragment);
            transaction.commitNow();
        }

        requestRole();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.history:
                HistoryFragment historyFragment = HistoryFragment.newInstance();
                FragmentTransaction historyTransaction = getTransaction(historyFragment);

                historyTransaction.addToBackStack(null);
                historyTransaction.commit();
                break;
            case R.id.dialer:
                DialerFragment dialerFragment = DialerFragment.newInstance();
                FragmentTransaction dialerTransaction = getTransaction(dialerFragment);

                dialerTransaction.addToBackStack(null);
                dialerTransaction.commit();
                break;
            default:
                break;
        }
    }

    public void requestRole() {
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

    private FragmentTransaction getTransaction(Fragment fragment) {
        fragment.setArguments(getIntent().getExtras());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);

        return transaction;
    }
}
