package com.sidia.ims.imsphone.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.service.linphone.LinphoneManager;
import com.sidia.ims.imsphone.service.linphone.LinphonePreferences;
import com.sidia.ims.imsphone.utils.linphone.LinphoneUtils;

import org.linphone.core.Core;
import org.linphone.core.ProxyConfig;

import java.util.ArrayList;
import java.util.List;

public class SideMenuAccountsListAdapter extends BaseAdapter {
    private final Context mContext;
    private List<ProxyConfig> proxy_list;

    SideMenuAccountsListAdapter(Context context) {
        mContext = context;
        proxy_list = new ArrayList<>();
        refresh();
    }

    @Override
    public int getCount() {
        if (proxy_list != null) {
            return proxy_list.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return proxy_list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View view;
        ProxyConfig lpc = (ProxyConfig) getItem(position);
        if (convertView != null) {
            view = convertView;
        } else {
            view =
                    LayoutInflater.from(mContext)
                            .inflate(R.layout.nav_account_cell, viewGroup, false);
        }

        ImageView status = view.findViewById(R.id.account_status);
        TextView address = view.findViewById(R.id.account_address);
        String sipAddress = lpc.getIdentityAddress().asStringUriOnly();

        address.setText(sipAddress);

        int nbAccounts = LinphonePreferences.instance().getAccountCount();
        int accountIndex;

        for (int i = 0; i < nbAccounts; i++) {
            String username = LinphonePreferences.instance().getAccountUsername(i);
            String domain = LinphonePreferences.instance().getAccountDomain(i);
            String id = "sip:" + username + "@" + domain;
            if (id.equals(sipAddress)) {
                accountIndex = i;
                view.setTag(accountIndex);
                break;
            }
        }
        status.setImageResource(LinphoneUtils.getStatusIconResource(lpc.getState()));
        return view;
    }

    private void refresh() {
        proxy_list = new ArrayList<>();
        Core core = LinphoneManager.getCore(mContext);
        for (ProxyConfig proxyConfig : core.getProxyConfigList()) {
            if (proxyConfig != core.getDefaultProxyConfig()) {
                proxy_list.add(proxyConfig);
            }
        }
    }
}
