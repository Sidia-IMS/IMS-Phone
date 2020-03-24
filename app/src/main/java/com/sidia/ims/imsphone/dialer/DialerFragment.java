package com.sidia.ims.imsphone.dialer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.snackbar.Snackbar;
import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.activities.ui.mainactivity.MainActivityViewModel;

import java.util.ArrayList;
import java.util.List;

public class DialerFragment extends Fragment implements View.OnClickListener {
    private static final int MULTIPLE_PERMISSIONS = 10;
    String[] PERMISSIONS = {
            android.Manifest.permission.CALL_PHONE
    };

    private EditText mAddress;
    private ConstraintLayout mLayout;
    private MainActivityViewModel mViewModel;

    public static DialerFragment newInstance() {
        return new DialerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_dailer, container, false);
        mAddress = getActivity().findViewById(R.id.address);
        mLayout = getActivity().findViewById(R.id.constraintLayout);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public void onClick(View view) {
        String currentText = mAddress.getText().toString();
        switch (view.getId()) {
            case R.id.erase:
                currentText = currentText.length() < 2 ? "" : currentText.substring(0, currentText.length() - 1);
                mAddress.setText(currentText);
                break;
            case R.id.start_call:
                String telUri = "tel:" + currentText;
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    checkPermissions(PERMISSIONS);
                    return;
                }

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse(telUri));
                startActivity(callIntent);
                break;
            default:
                ImageButton digit = (ImageButton) view;
                currentText += digit.getContentDescription().toString();
                mAddress.setText(currentText);
                break;
        }
    }

    private  boolean checkPermissions(String permissionsNeeded[]) {
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
            ActivityCompat.requestPermissions(getActivity(), permissionsNeeded, MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:
                if (grantResults.length > 0) {
                    boolean allPermissionsGranted = true;
                    List<String> missingPermissions = new ArrayList<>();

                    for (int i = 0; i < permissions.length; i++) {
                        boolean permissionGranted= grantResults[i] == PackageManager.PERMISSION_GRANTED;
                        if (!permissionGranted) {
                            missingPermissions.add(permissions[i]);
                        }
                        allPermissionsGranted = allPermissionsGranted && permissionGranted;
                    }

                    if (!allPermissionsGranted) {
                        Snackbar.make(mLayout, "The app has not been granted permissions:\n. Hence, it cannot function properly. Please consider granting it this permission", Snackbar.LENGTH_LONG);
                    }
                }
                break;
        }
    }
}
