package com.sidia.ims.imsphone.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.activities.ui.mainactivity.ImsPhoneViewModel;
import com.sidia.ims.imsphone.model.ImsPhoneCallLog;
import com.sidia.ims.imsphone.service.linphone.LinphoneContext;
import com.sidia.ims.imsphone.service.linphone.LinphoneService;

import org.linphone.mediastream.Version;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ImsPhoneUtils {
    private static final String LOG_TAG = "SP-Utils";
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    public static List<ImsPhoneCallLog> readCallLog(Activity activity) {
        List<ImsPhoneCallLog>  result = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            ImsPhoneViewModel.checkPermissions(activity, ImsPhoneViewModel.PERMISSIONS);
            return result;
        }
        Cursor cursor = activity.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                null, null, null, null);

        while (cursor.moveToNext()) {
            String address = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));// for  number
            //String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));// for name
            //String duration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION));// for duration
            int type = Integer.parseInt(cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE)));

            ImsPhoneCallLog obj = new ImsPhoneCallLog();
            obj.setAddress(address);
            obj.setType(type);

            result.add(obj);
        }
        return result;
    }

    public static void dispatchOnUIThread(Runnable r) {
        sHandler.post(r);
    }

    public static String getDeviceName(Context context) {
        if (Version.sdkAboveOrEqual(25)) {
            String name = Settings.Global.getString(
                    context.getContentResolver(), Settings.Global.DEVICE_NAME);

            if (name != null) {
                return name;
            }
        }

        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    public static Dialog getDialog(Context context, String text) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Drawable d = new ColorDrawable(ContextCompat.getColor(context, R.color.dark_grey_color));
        d.setAlpha(200);
        dialog.setContentView(R.layout.dialog);
        dialog.getWindow()
                .setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(d);

        TextView customText = dialog.findViewById(R.id.dialog_message);
        customText.setText(text);
        return dialog;
    }

    public static void displayErrorAlert(String msg, Context ctxt) {
        if (ctxt != null && msg != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);
            builder.setMessage(msg)
                    .setCancelable(false)
                    .setNeutralButton(ctxt.getString(R.string.ok), null)
                    .show();
        }
    }

    public static String getString(Context context, int key) {
        return context.getString(key);
    }

    public static void ensureServiceIsRunning(Context ctx) {
        if (!LinphoneService.isReady()) {
            if (!LinphoneContext.isReady()) {
                new LinphoneContext(ctx.getApplicationContext());
                LinphoneContext.instance(ctx).start(false);
                Log.i(LOG_TAG, "[Generic Activity] Context created & started");
            }

            Log.i(LOG_TAG,"[Generic Activity] Starting Service");
            try {
                ctx.startService(new Intent().setClass(ctx, LinphoneService.class));
            } catch (IllegalStateException ise) {
                Log.e(LOG_TAG, "[Generic Activity] Couldn't start service, exception: ", ise);
            }
        }
    }
}
