package com.sidia.ims.imsphone.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.sidia.ims.imsphone.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText mAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAddress = findViewById(R.id.address);
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
                String telUri = "tel:" + currentText
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse(telUri));
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivity(callIntent);
            default:
                ImageButton digit = (ImageButton) view;
                currentText += digit.getContentDescription().toString();
                mAddress.setText(currentText);
                break;
        }
    }
}
