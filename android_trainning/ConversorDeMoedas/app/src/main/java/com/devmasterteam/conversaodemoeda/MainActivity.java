package com.devmasterteam.conversaodemoeda;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ViewHolder mViewHolder = new ViewHolder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        this.mViewHolder.editReal = this.findViewById(R.id.edit_real);
        this.mViewHolder.textDollar = this.findViewById(R.id.text_dollar);
        this.mViewHolder.textEuro = this.findViewById(R.id.text_euro);
        this.mViewHolder.buttonCalculate = this.findViewById(R.id.button_calculate);

        this.clearValues();

        this.mViewHolder.buttonCalculate.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.button_calculate) {

            String value = this.mViewHolder.editReal.getText().toString();
            if ("".equals(value)) {
                Toast.makeText(this, this.getString(R.string.informe_valor), Toast.LENGTH_LONG).show();
            } else {
                // Converte o valor informado
                Double real = Double.valueOf(value);

                // Converte valores
                this.mViewHolder.textDollar.setText(String.format("%.2f", real / 4));
                this.mViewHolder.textEuro.setText(String.format("%.2f", real / 5));
            }
        }
    }

    private void clearValues() {
        this.mViewHolder.textDollar.setText("");
        this.mViewHolder.textEuro.setText("");
    }

    private static class ViewHolder {
        private EditText editReal;
        private TextView textDollar;
        private TextView textEuro;
        private Button buttonCalculate;
    }
}