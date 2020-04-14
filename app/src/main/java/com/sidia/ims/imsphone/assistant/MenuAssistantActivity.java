package com.sidia.ims.imsphone.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.sidia.ims.imsphone.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MenuAssistantActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.account_creation:
                Intent creationIntent = new Intent(this, PhoneAccountCreationAssistantActivity.class);
                startActivity(creationIntent);
                break;
            case R.id.account_connection:
                Intent connectionIntent = new Intent(this, AccountConnectionAssistantActivity.class);
                startActivity(connectionIntent);
                break;
            default:
                break;
        }
    }
}
