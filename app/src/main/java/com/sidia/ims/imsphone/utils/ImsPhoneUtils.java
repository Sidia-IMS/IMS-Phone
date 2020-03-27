package com.sidia.ims.imsphone.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

import com.sidia.ims.imsphone.activities.ui.mainactivity.ImsPhoneViewModel;
import com.sidia.ims.imsphone.model.ImsPhoneCallLog;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;

public class ImsPhoneUtils {

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
}
