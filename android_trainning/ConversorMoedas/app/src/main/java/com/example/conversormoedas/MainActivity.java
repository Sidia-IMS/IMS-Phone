package com.example.conversormoedas;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();
    private ViewHolder mViewHolder = new ViewHolder();
    private JsonReader jsonReader;
    JSONObject jsonObject;

    private boolean isSwitchCheck = false;
    private RequestQueue mRequestQueue;
    private StringRequest mStringRequest;
    String url = "https://api.exchangeratesapi.io/latest?base=BRL";
    double USD = 0;
    double EUR = 0;
    String date = "";
    String[] separateDates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mViewHolder.textValue = findViewById(R.id.edit_value);
        this.mViewHolder.textDolar = findViewById(R.id.text_dolar);
        this.mViewHolder.textEuro = findViewById(R.id.text_euro);
        this.mViewHolder.textCotacoes = findViewById(R.id.text_cotacoes);
        this.mViewHolder.buttonCalculate = findViewById(R.id.buttonCalculate);
        this.mViewHolder.swithOnline = findViewById(R.id.switch_online);

        this.mViewHolder.textCotacoes.setText("");

        this.mViewHolder.buttonCalculate.setOnClickListener(this);
        this.mViewHolder.swithOnline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    sendAndRequestResponse();
                } else {
                    mViewHolder.textCotacoes.setText("");
                    date = "";
                }
            }
        });
        this.clearValues();

    }

    private void updateCotacaoText() {
        try {
            JSONObject ratesUSD = (JSONObject) jsonObject.get("rates");
            USD = (Double) ratesUSD.get("USD");
            JSONObject ratesEUR = (JSONObject) jsonObject.get("rates");
            EUR = (Double) ratesEUR.get("EUR");
            date = (String) jsonObject.get("date");
            separateDates = date.split("-");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.mViewHolder.textCotacoes.setText(String.format("Baseado nas cotações de %s/%s/%s\nDólar de R$%.2f\nEuro de R$%.2f",
                separateDates[2],separateDates[1],separateDates[0], 1/USD, 1/EUR));

    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.buttonCalculate) {
            String value = this.mViewHolder.textValue.getText().toString();
            if ("".equals(value)) {
                Toast.makeText(this, this.getString(R.string.toastErrorMessage), Toast.LENGTH_LONG).show();
            } else if (date.equals("")){
                Double real = Double.valueOf(value);
                this.mViewHolder.textCotacoes.setText(String.format(String.format("Baseado nas cotações fictícias de\nDólar de R$%.2f\nEuro de R$%.2f", 1/0.25, 1/0.2)));
                this.mViewHolder.textDolar.setText(String.format("%.2f", real / 4));
                this.mViewHolder.textEuro.setText(String.format("%.2f", real / 5));
            } else{
                double real = Double.valueOf(value);
                this.mViewHolder.textDolar.setText(String.format("%.2f", real / (1/USD)));
                this.mViewHolder.textEuro.setText(String.format("%.2f", real / (1/EUR)));
            }
        }
    }

    private void sendAndRequestResponse() {

        //RequestQueue initialized
        mRequestQueue = Volley.newRequestQueue(this);

        //String Request initialized
        mStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    jsonObject = new JSONObject(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                updateCotacaoText();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.i(TAG, "Error :" + error.toString());
            }
        });
        mRequestQueue.add(mStringRequest);
    }

    private void clearValues() {
        this.mViewHolder.textDolar.setText("");
        this.mViewHolder.textEuro.setText("");
    }


    private static class ViewHolder {
        EditText textValue;
        TextView textDolar;
        TextView textEuro;
        Button buttonCalculate;
        Button buttonRequest;
        TextView textCotacoes;
        Switch swithOnline;
    }
}

