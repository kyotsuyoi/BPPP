package com.bppp.Main;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.bppp.CommonClasses.ApiClient;
import com.bppp.CommonClasses.Handler;
import com.bppp.CommonClasses.SavedUser;
import com.bppp.R;
import com.bppp.android.IntentIntegrator;
import com.bppp.android.IntentResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private SavedUser SU = SavedUser.getSavedUser();
    private com.bppp.CommonClasses.Handler Handler = new Handler();
    private int R_ID = R.id.Button;

    private TextView textViewStore, textViewPrice, textViewRequest;
    private EditText editTextEAN, editTextID;

    private JsonArray jsonArray;
    private JsonArray shopList;
    private int SelectedShop;
    private Spinner spinner;

    private MainInterface mainInterface = ApiClient.getApiClient().create(MainInterface.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Busca Preço");
        getSupportActionBar().setSubtitle("Pesquise o produto");

        textViewStore = findViewById(R.id.TextView_Store);
        textViewPrice = findViewById(R.id.TextView_Price);
        textViewRequest = findViewById(R.id.TextView_Request);
        editTextEAN = findViewById(R.id.EditText_EAN);
        editTextID = findViewById(R.id.EditText_ID);
        spinner = findViewById(R.id.spinner);

        SetButton();
        SetListenerForTextView();
        SetSpinner();
    }

    private void SetButton(){
        findViewById(R.id.Button).setOnClickListener(v->{
            try {
                hideKeyboard(MainActivity.this, editTextEAN.getRootView());
                if(SelectedShop==0){
                    Handler.ShowSnack("Selecione a loja",null,this, R_ID, false);
                    return;
                }
                if(!editTextEAN.getText().toString().equalsIgnoreCase("")) {
                    GetByEAN(editTextEAN.getText().toString());
                    editTextEAN.setText("");
                }else if(!editTextID.getText().toString().equalsIgnoreCase("")){
                    GetByID(editTextID.getText().toString());
                    editTextID.setText("");
                }else{
                    IntentIntegrator integrator = new IntentIntegrator(this);
                    integrator.initiateScan();
                }
                Reset();
            }catch (Exception e){
                Handler.ShowSnack("Houve um erro","SetButton: "+e.getMessage(),this, R_ID, true);
            }
        });
    }

    public void SetListenerForTextView(){
        editTextEAN.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                editTextID.setText("");
            }
        });
        editTextID.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                editTextEAN.setText("");
            }
        });
    }

    private void Reset(){
        Objects.requireNonNull(getSupportActionBar()).setTitle("Busca Preço");
        getSupportActionBar().setSubtitle("Pesquise o produto");
        textViewStore.setText("");
        textViewPrice.setText("");
        textViewRequest.setText("");
        editTextEAN.setText("");
        editTextID.setText("");
    }

    private void GetByID(String PLU){
        try {
            Call<JsonObject> call = mainInterface.GetByID(4,SU.getSession(),SelectedShop,PLU);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try{
                        if (!Handler.isRequestError(response,MainActivity.this,R_ID)) {
                            JsonObject jsonObject = response.body();
                            jsonArray = jsonObject.get("data").getAsJsonArray();
                            jsonObject = jsonArray.get(0).getAsJsonObject();
                            textViewPrice.setText(jsonObject.get("price").getAsString());
                            textViewStore.setText(jsonObject.get("store").getAsString());
                            getSupportActionBar().setTitle(jsonObject.get("plu").getAsString());
                            getSupportActionBar().setSubtitle(jsonObject.get("description").getAsString());
                        }
                    }catch (Exception e) {
                        Handler.ShowSnack("Houve um erro","MainActivity.GetByID.onResponse: " + e.getMessage(), MainActivity.this, R_ID,true);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Handler.ShowSnack("Houve um erro","MainActivity.GetByID.onFailure: " + t.toString(), MainActivity.this, R_ID,true);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetByID: " + e.getMessage(), MainActivity.this, R_ID,true);
        }
    }

    private void GetByEAN(String EAN){
        try {
            Call<JsonObject> call = mainInterface.GetByEAN(4,SU.getSession(),SelectedShop,EAN);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try{
                        if (!Handler.isRequestError(response,MainActivity.this,R_ID)) {
                            JsonObject jsonObject = response.body();
                            jsonArray = jsonObject.get("data").getAsJsonArray();
                            jsonObject = jsonArray.get(0).getAsJsonObject();
                            textViewPrice.setText(jsonObject.get("price").getAsString());
                            textViewStore.setText(jsonObject.get("store").getAsString());
                            getSupportActionBar().setTitle(jsonObject.get("plu").getAsString());
                            getSupportActionBar().setSubtitle(jsonObject.get("description").getAsString());
                        }
                    }catch (Exception e) {
                        Handler.ShowSnack("Houve um erro","MainActivity.GetByEAN.onResponse: " + e.getMessage(), MainActivity.this, R_ID,true);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Handler.ShowSnack("Houve um erro","MainActivity.GetByEAN.onFailure: " + t.toString(), MainActivity.this, R_ID,true);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetByEAN: " + e.getMessage(), MainActivity.this, R_ID,true);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        assert scanningResult != null;
        if (scanningResult.getContents() != null) {
            try {
                String scanBar = scanningResult.getContents();
                GetByEAN(scanBar);
            }catch (Exception e){
                Handler.ShowSnack("Houve um erro","MainActivity.onActivityResult: " + e.getMessage(), MainActivity.this, R_ID,true);
            }
        }
    }

    public static void hideKeyboard(Context context, View view) {
        try {
            InputMethodManager keyboard = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            assert keyboard != null;
            keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void GetShop(){
        try {
            Call<JsonObject> call = mainInterface.GetShop(4,SU.getSession(), 1);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!Handler.isRequestError(response,MainActivity.this,R_ID)){
                        try{
                            JsonObject jsonObject = response.body();
                            shopList = jsonObject.get("data").getAsJsonArray();

                            ArrayList arrayList = new ArrayList<>();
                            arrayList.add("");
                            for (int i = 0; i < shopList.size(); i++) {

                                int id = shopList.get(i).getAsJsonObject().get("id").getAsInt();
                                String description = shopList.get(i).getAsJsonObject().get("description").getAsString();

                                arrayList.add(id + " - " + description);
                            }

                            ArrayAdapter<String> arrayAdapter= new ArrayAdapter<>(MainActivity.this,R.layout.support_simple_spinner_dropdown_item,arrayList);

                            spinner.setAdapter(arrayAdapter);
                        }catch (Exception e) {
                            Handler.ShowSnack("Houve um erro","MainActivity.GetShop.onResponse: " + e.getMessage(), MainActivity.this, R_ID,true);
                        }
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Handler.ShowSnack("Houve um erro","MainActivity.GetShop.onFailure: " + t.toString(), MainActivity.this, R_ID,true);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetShop: " + e.getMessage(), MainActivity.this, R_ID,true);
        }
    }

    private void SetSpinner() {
        GetShop();
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    Reset();
                    ((TextView) parent.getChildAt(0)).setTextColor(getApplicationContext().getColor(R.color.textColor));
                    if(position!=0) {
                        SelectedShop = shopList.get(position-1).getAsJsonObject().get("id").getAsInt();
                    }else{
                        SelectedShop = 0;
                    }
                } catch (Exception e) {
                    Handler.ShowSnack("Houve um erro", "MainActivity.SetFilterSpinner.spinnerCompany: " + e.getMessage(), MainActivity.this, R_ID, true);
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

}