package com.example.festadefimdeano.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.example.festadefimdeano.R;
import com.example.festadefimdeano.constant.FimDeAnoConstants;
import com.example.festadefimdeano.data.SecurityPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ViewHolder mViewHolder = new ViewHolder();
    private static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private SecurityPreferences mSecurityPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mViewHolder.textToday = findViewById(R.id.text_today);
        this.mViewHolder.textDaysLeft = findViewById(R.id.text_days_left);
        this.mViewHolder.buttonConfirm = findViewById(R.id.button_confirm);
        this.mViewHolder.buttonConfirm.setOnClickListener(this);
        this.mSecurityPreferences = new SecurityPreferences(this);

        // datas
        this.mViewHolder.textToday.setText(sSimpleDateFormat.format(Calendar.getInstance().getTime()));
        this.mViewHolder.textDaysLeft.setText(getDaysLeft() + " dias");
        // String daysLeft = String.format("%s %s", String.valueOf(this.getDaysLeft()), getString(R.string.dias));
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.verifierPresence();
    }

    private void verifierPresence() {
        String presence = this.mSecurityPreferences.getStoredString(FimDeAnoConstants.PRESENCE_KEY);
        if(presence.equals("")){
            this.mViewHolder.buttonConfirm.setText(getString(R.string.nao_confirmado));
        } else if (presence.equals(FimDeAnoConstants.CONFIRMATION_YES)) {
            this.mViewHolder.buttonConfirm.setText(getString(R.string.sim));
        } else {
            this.mViewHolder.buttonConfirm.setText(getString(R.string.nao));
        }
    }

    private int getDaysLeft(){
        // data de hoje
        Calendar calendarToday = Calendar.getInstance();
        int today = calendarToday.get(Calendar.DAY_OF_YEAR);

        // dia máximo do ano
        Calendar calendarLastDay = Calendar.getInstance();
        int dayMax = calendarLastDay.getActualMaximum(Calendar.DAY_OF_YEAR);

        return dayMax-today;
    }

    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.button_confirm){
            String presence = this.mSecurityPreferences.getStoredString(FimDeAnoConstants.PRESENCE_KEY);
            Intent intent = new Intent(getApplicationContext(), DetailsActivity.class); // poderia achar o context usando o proprio this
            intent.putExtra(FimDeAnoConstants.PRESENCE_KEY, presence);
            startActivity(intent);
        }
    }

    private static class ViewHolder {
        TextView textToday;
        TextView textDaysLeft;
        Button buttonConfirm;
    }
}
