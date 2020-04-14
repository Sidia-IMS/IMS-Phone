package com.sidia.ims.imsphone.service.linphone;

import android.content.Context;
import android.util.Log;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.utils.ImsPhoneUtils;

import org.linphone.core.Config;
import org.linphone.core.Core;
import org.linphone.core.Factory;
import org.linphone.core.ProxyConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LinphonePreferences {
    private static final String LOG_TAG = "SP-LinphonePreferences";

    private static final int LINPHONE_CORE_RANDOM_PORT = -1;
    private static final String LINPHONE_DEFAULT_RC = "/.linphonerc";
    private static final String LINPHONE_FACTORY_RC = "/linphonerc";
    private static final String LINPHONE_LPCONFIG_XSD = "/lpconfig.xsd";
    private static final String DEFAULT_ASSISTANT_RC = "/default_assistant_create.rc";
    private static final String LINPHONE_ASSISTANT_RC = "/linphone_assistant_create.rc";


    private static LinphonePreferences sInstance;

    private Context mContext;
    private String mBasePath;

    public static synchronized LinphonePreferences instance() {
        if (sInstance == null) {
            sInstance = new LinphonePreferences();
        }
        return sInstance;
    }

    public void setContext(Context c) {
        mContext = c;
        mBasePath = mContext.getFilesDir().getAbsolutePath();
        try {
            copyAssetsFromPackage();
        } catch (IOException ioe) {

        }
    }

    public Config getConfig() {
        Core core = getLc();
        if (core != null) {
            return core.getConfig();
        }

        if (!LinphoneContext.isReady()) {
            File linphonerc = new File(mBasePath + "/.linphonerc");
            if (linphonerc.exists()) {
                return Factory.instance().createConfig(linphonerc.getAbsolutePath());
            } else if (mContext != null) {
                InputStream inputStream =
                        mContext.getResources().openRawResource(R.raw.linphonerc_default);
                InputStreamReader inputreader = new InputStreamReader(inputStream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                StringBuilder text = new StringBuilder();
                String line;
                try {
                    while ((line = buffreader.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                } catch (IOException ioe) {
                    Log.e(LOG_TAG, ioe.getMessage());
                }
                return Factory.instance().createConfigFromString(text.toString());
            }
        } else {
            return Factory.instance().createConfig(getLinphoneDefaultConfig());
        }
        return null;
    }

    public void setPushNotificationEnabled(boolean enable) {
        if (getConfig() == null) return;
        getConfig().setBool("app", "push_notification", enable);

        Core core = getLc();
        if (core == null) {
            return;
        }

        if (enable) {
            // Add push infos to exisiting proxy configs
            String regId = getPushNotificationRegistrationID();
            String appId = ImsPhoneUtils.getString(mContext, R.string.gcm_defaultSenderId);
            if (regId != null && core.getProxyConfigList().length > 0) {
                for (ProxyConfig lpc : core.getProxyConfigList()) {
                    if (lpc == null) continue;
                    if (!lpc.isPushNotificationAllowed()) {
                        lpc.edit();
                        lpc.setContactUriParameters(null);
                        lpc.done();
                        if (lpc.getIdentityAddress() != null)
                            Log.d(LOG_TAG,
                                    "[Push Notification] infos removed from proxy config "
                                            + lpc.getIdentityAddress().asStringUriOnly());
                    } else {
                        String contactInfos =
                                "app-id="
                                        + appId
                                        + ";pn-type="
                                        + ImsPhoneUtils.getString(mContext, R.string.push_type)
                                        + ";pn-timeout=0"
                                        + ";pn-tok="
                                        + regId
                                        + ";pn-silent=1";
                        String prevContactParams = lpc.getContactParameters();
                        if (prevContactParams == null
                                || prevContactParams.compareTo(contactInfos) != 0) {
                            lpc.edit();
                            lpc.setContactUriParameters(contactInfos);
                            lpc.done();
                            if (lpc.getIdentityAddress() != null)
                                Log.d(LOG_TAG,
                                        "[Push Notification] infos added to proxy config "
                                                + lpc.getIdentityAddress().asStringUriOnly());
                        }
                    }
                }
                Log.i(LOG_TAG,
                        "[Push Notification] Refreshing registers to ensure token is up to date: "
                                + regId);
                core.refreshRegisters();
            }
        } else {
            if (core.getProxyConfigList().length > 0) {
                for (ProxyConfig lpc : core.getProxyConfigList()) {
                    lpc.edit();
                    lpc.setContactUriParameters(null);
                    lpc.done();
                    if (lpc.getIdentityAddress() != null)
                        Log.d(LOG_TAG,
                                "[Push Notification] infos removed from proxy config "
                                        + lpc.getIdentityAddress().asStringUriOnly());
                }
                core.refreshRegisters();
            }
        }
    }

    // App settings
    public boolean isFirstLaunch() {
        if (getConfig() == null) return true;
        return getConfig().getBool("app", "first_launch", true);
    }

    public String getLinphoneDefaultConfig() {
        return mBasePath + LINPHONE_DEFAULT_RC;
    }

    public boolean isAutoAnswerEnabled() {
        if (getConfig() == null) return false;
        return getConfig().getBool("app", "auto_answer", false);
    }

    public String getLinphoneFactoryConfig() {
        return mBasePath + LINPHONE_FACTORY_RC;
    }


    public boolean isPushNotificationEnabled() {
        if (getConfig() == null) return true;
        return getConfig().getBool("app", "push_notification", true);
    }

    public String getLinphoneDynamicConfigFile() {
        return mBasePath + LINPHONE_ASSISTANT_RC;
    }

    public String getDefaultDynamicConfigFile() {
        return mBasePath + DEFAULT_ASSISTANT_RC;
    }

    private Core getLc() {
        if (!LinphoneContext.isReady()) return null;

        return LinphoneManager.getCore(mContext);
    }

    private void copyAssetsFromPackage() throws IOException {
        copyIfNotExist(R.raw.linphonerc_default, getLinphoneDefaultConfig());
        copyFromPackage(R.raw.linphonerc_factory, new File(getLinphoneFactoryConfig()).getName());
        copyIfNotExist(R.raw.lpconfig, mBasePath + LINPHONE_LPCONFIG_XSD);
        copyFromPackage(
                R.raw.default_assistant_create,
                new File(mBasePath + DEFAULT_ASSISTANT_RC).getName());
        copyFromPackage(
                R.raw.linphone_assistant_create,
                new File(mBasePath + LINPHONE_ASSISTANT_RC).getName());
    }

    private void copyIfNotExist(int ressourceId, String target) throws IOException {
        File lFileToCopy = new File(target);
        if (!lFileToCopy.exists()) {
            copyFromPackage(ressourceId, lFileToCopy.getName());
        }
    }

    public String getDeviceName(Context context) {
        String defaultValue = ImsPhoneUtils.getDeviceName(context);
        if (getConfig() == null) return defaultValue;
        return getConfig().getString("app", "device_name", defaultValue);
    }

    private void copyFromPackage(int ressourceId, String target) throws IOException {
        FileOutputStream lOutputStream = mContext.openFileOutput(target, 0);
        InputStream lInputStream = mContext.getResources().openRawResource(ressourceId);
        int readByte;
        byte[] buff = new byte[8048];
        while ((readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff, 0, readByte);
        }
        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }

    private String getPushNotificationRegistrationID() {
        if (getConfig() == null) return null;
        return getConfig().getString("app", "push_notification_regid", null);
    }

    public boolean isLinkPopupEnabled() {
        if (getConfig() == null) return true;
        return getConfig().getBool("app", "link_popup_enabled", true);
    }

    public void enableLinkPopup(boolean enable) {
        if (getConfig() == null) return;
        getConfig().setBool("app", "link_popup_enabled", enable);
    }

    public String getLinkPopupTime() {
        if (getConfig() == null) return null;
        return getConfig().getString("app", "link_popup_time", null);
    }

    public void setLinkPopupTime(String date) {
        if (getConfig() == null) return;
        getConfig().setString("app", "link_popup_time", date);
    }

    public String getXmlrpcUrl() {
        if (getConfig() == null) return null;
        return getConfig().getString("assistant", "xmlrpc_url", null);
    }

    public void setServiceNotificationVisibility(boolean enable) {
        if (getConfig() == null) return;
        getConfig().setBool("app", "show_service_notification", enable);
    }

    public void firstLaunchSuccessful() {
        getConfig().setBool("app", "first_launch", false);
    }
}
