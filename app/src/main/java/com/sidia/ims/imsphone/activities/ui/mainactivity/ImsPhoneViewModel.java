package com.sidia.ims.imsphone.activities.ui.mainactivity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModel;

public class ImsPhoneViewModel extends ViewModel {
    public static final int MULTIPLE_PERMISSIONS = 10;

    public static final String[] PERMISSIONS = {
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_NUMBERS
    };

    public static boolean checkPermissions(Activity activity, String permissionsNeeded[]) {
        /*
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : PERMISSIONS) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        */

        if (permissionsNeeded.length > 0) {
            ActivityCompat.requestPermissions(activity, permissionsNeeded, MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    public static boolean requestResult(String permissions[], int[] grantResults) {
        boolean allPermissionsGranted = true;
        List<String> missingPermissions = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++) {
            boolean permissionGranted = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
            if (!permissionGranted) {
                missingPermissions.add(permissions[i]);
            }
            allPermissionsGranted = allPermissionsGranted && permissionGranted;
        }

        return allPermissionsGranted;
    }
}
