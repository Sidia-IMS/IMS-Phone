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
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.snackbar.Snackbar;
import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.activities.ui.mainactivity.ImsPhoneViewModel;

import java.util.ArrayList;
import java.util.List;

public class DialerFragment extends Fragment implements View.OnClickListener {
    public static final String TAG = "dialer";
    private int[] digitsId = {
            R.id.erase,
            R.id.start_call,
            R.id.Digit00,
            R.id.Digit1,
            R.id.Digit2,
            R.id.Digit3,
            R.id.Digit4,
            R.id.Digit5,
            R.id.Digit6,
            R.id.Digit7,
            R.id.Digit8,
            R.id.Digit9,
    };

    private EditText mAddress;
    private ConstraintLayout mLayout;
    private ImsPhoneViewModel mViewModel;
    private View viewInflate;

    public static DialerFragment newInstance() {
        return new DialerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewInflate =  inflater.inflate(R.layout.fragment_dailer, container, false);
        return viewInflate;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ImsPhoneViewModel.class);
        mAddress = viewInflate.findViewById(R.id.address);
        mLayout = viewInflate.findViewById(R.id.constraintLayout);

        for (int id : digitsId) {
            ImageButton btn = viewInflate.findViewById(id);
            btn.setOnClickListener(this);
        }

        ImsPhoneViewModel.checkPermissions(getActivity(), ImsPhoneViewModel.PERMISSIONS);
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
                    ImsPhoneViewModel.checkPermissions(getActivity(), ImsPhoneViewModel.PERMISSIONS);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ImsPhoneViewModel.MULTIPLE_PERMISSIONS:
                if (grantResults.length > 0) {
                    boolean allPermissionsGranted = mViewModel.requestResult(permissions, grantResults);

                    if (!allPermissionsGranted) {
                        Snackbar.make(mLayout, "The app has not been granted permissions:\n. Hence, it cannot function properly. Please consider granting it this permission", Snackbar.LENGTH_LONG);
                    }
                }
                break;
        }
    }
}
