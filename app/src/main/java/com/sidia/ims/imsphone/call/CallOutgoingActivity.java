package com.sidia.ims.imsphone.call;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.activities.MainActivity;
import com.sidia.ims.imsphone.telephony.OngoingCall;

import androidx.appcompat.app.AppCompatActivity;

public class CallOutgoingActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView contactNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_outgoing);

        contactNumber = findViewById(R.id.contact_number);
        if (getIntent().getData() != null) {
            contactNumber.setText(getIntent().getData().getSchemeSpecificPart());
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.outgoing_hang_up:
                OngoingCall.hangup();
                startActivity(new Intent(this, MainActivity.class));
                break;
            default:
                break;
        }
    }
}
