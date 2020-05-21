package com.sidia.ims.imsphone.menu;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.activities.MainActivity;
import com.sidia.ims.imsphone.service.linphone.LinphoneContext;
import com.sidia.ims.imsphone.service.linphone.LinphoneManager;
import com.sidia.ims.imsphone.service.linphone.LinphonePreferences;
import com.sidia.ims.imsphone.utils.linphone.LinphoneUtils;

import org.linphone.core.Core;
import org.linphone.core.ProxyConfig;

import androidx.constraintlayout.widget.ConstraintLayout;

public class SideMenuViewModel {
    MainActivity mActivity;
    private ConstraintLayout mDefaultAccount;
    private ListView mAccountsList;

    public SideMenuViewModel (MainActivity activity, View headerView) {
        mActivity = activity;
        mAccountsList = headerView.findViewById(R.id.accounts_list);
        mDefaultAccount = headerView.findViewById(R.id.default_account);
    }


    public void displayAccountsInSideMenu() {
        Core core = LinphoneManager.getCore(mActivity);
        if (core != null
                && core.getProxyConfigList() != null
                && core.getProxyConfigList().length > 1) {
            mAccountsList.setVisibility(View.VISIBLE);
            mAccountsList.setAdapter(new SideMenuAccountsListAdapter(mActivity));
            mAccountsList.setOnItemClickListener(
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(
                                AdapterView<?> adapterView, View view, int i, long l) {
                            if (view != null && view.getTag() != null) {
                                int position = Integer.parseInt(view.getTag().toString());
                                mActivity.showAccountSettings(position);
                            }
                        }
                    });
        } else {
            mAccountsList.setVisibility(View.GONE);
        }
        displayMainAccount();
    }

    private void displayMainAccount() {
        ImageView status = mDefaultAccount.findViewById(R.id.main_account_status);
        TextView address = mDefaultAccount.findViewById(R.id.main_account_address);
        TextView displayName = mDefaultAccount.findViewById(R.id.main_account_display_name);

        if (!LinphoneContext.isReady() || LinphoneManager.getCore(mActivity) == null) return;

        ProxyConfig proxy = LinphoneManager.getCore(mActivity).getDefaultProxyConfig();
        if (proxy == null) {
            displayName.setText(mActivity.getString(R.string.no_account));
            status.setVisibility(View.GONE);
            address.setText("");
            mDefaultAccount.setOnClickListener(null);
        } else {
            address.setText(proxy.getIdentityAddress().asStringUriOnly());
            displayName.setText(LinphoneUtils.getAddressDisplayName(proxy.getIdentityAddress()));
            status.setImageResource(LinphoneUtils.getStatusIconResource(proxy.getState()));
            status.setVisibility(View.VISIBLE);

            mDefaultAccount.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            (mActivity)
                                    .showAccountSettings(
                                            LinphonePreferences.instance()
                                                    .getDefaultAccountIndex());
                        }
                    });

        }
    }


}
