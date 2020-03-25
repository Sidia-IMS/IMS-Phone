package com.sidia.ims.imsphone.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.View;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.dialer.DialerFragment;
import com.sidia.ims.imsphone.history.HistoryFragment;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ConstraintLayout footerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        footerLayout = findViewById(R.id.footerLayout);
        if (savedInstanceState == null) {
            DialerFragment dialerFragment = DialerFragment.newInstance();
            FragmentTransaction transaction = getTransaction(dialerFragment);
            transaction.commitNow();
        }
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

    private FragmentTransaction getTransaction(Fragment fragment) {
        fragment.setArguments(getIntent().getExtras());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);

        return transaction;
    }
}
