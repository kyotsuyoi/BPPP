package com.bppp.Main;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bppp.CommonClasses.ApiClient;
import com.bppp.CommonClasses.Handler;
import com.bppp.CommonClasses.RecyclerItemClickListener;
import com.bppp.CommonClasses.SavedUser;
import com.bppp.Login.LoginActivity;
import com.bppp.R;
import com.bppp.android.IntentIntegrator;
import com.bppp.android.IntentResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

public class MainActivity extends AppCompatActivity {

    private final SavedUser SU = SavedUser.getSavedUser();
    private final com.bppp.CommonClasses.Handler Handler = new Handler();
    private final int R_ID = R.id.Button;
    private static final String BPPP_PREFERENCES = "BPPP_PREFERENCES";
    //private static final int CompanyID = 1; //Only Peg Pese

    private TextView textViewStore, textViewPrice, textViewRequest;
    private EditText editTextEAN, editTextID;

    private JsonArray jsonArray;
    //private JsonArray shopList;
    private JsonArray permissionsList;
    private int SelectedShop;
    private Spinner spinner;

    private final MainInterface mainInterface = ApiClient.getApiClient().create(MainInterface.class);

    private Dialog DialogSearch;
    private MainAdapter adapter;

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

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setMessage("Deseja realmente efetuar logoff?");
        alert.setCancelable(false);
        alert.setPositiveButton("Sim", (dialog, which) -> {
            getSharedPreferences(BPPP_PREFERENCES, 0).edit().clear().apply();
            SavedUser.setSavedUser(null);

            Intent intent  = new Intent(getApplicationContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        alert.setNegativeButton("Não", (dialog, which) -> dialog.cancel());
        alert.setTitle("BPPP");
        alert.create();
        alert.show();
    }

    public void onResume() {
        //GetApplicationAccessFunction();
        super.onResume();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void SetButton(){
        findViewById(R.id.Button).setOnClickListener(v->{
            try {
                hideKeyboard(MainActivity.this, editTextEAN.getRootView());
                if(SelectedShop==0){
                    Handler.ShowSnack("Selecione a loja",null,this, R_ID);
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
                Handler.ShowSnack("Houve um erro","SetButton: "+e.getMessage(),
                        this, R_ID);
            }
        });

        findViewById(R.id.imageView_Search).setOnClickListener(v->{
            SetDialogSearch();
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
                            textViewPrice.setText("R$"+jsonObject.get("price").getAsString());
                            textViewStore.setText(jsonObject.get("store").getAsString());
                            getSupportActionBar().setTitle(jsonObject.get("plu").getAsString());
                            getSupportActionBar().setSubtitle(jsonObject.get("description").getAsString());

                            SetColor(jsonObject);
                        }
                    }catch (Exception e) {
                        Handler.ShowSnack("Houve um erro","MainActivity.GetByID.onResponse: " + e.getMessage(), MainActivity.this, R_ID);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Handler.ShowSnack("Houve um erro","MainActivity.GetByID.onFailure: " + t.toString(), MainActivity.this, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetByID: " + e.getMessage(), MainActivity.this, R_ID);
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
                            textViewPrice.setText("R$"+jsonObject.get("price").getAsString());
                            textViewStore.setText(jsonObject.get("store").getAsString());
                            getSupportActionBar().setTitle(jsonObject.get("plu").getAsString());
                            getSupportActionBar().setSubtitle(jsonObject.get("description").getAsString());

                            SetColor(jsonObject);
                        }
                    }catch (Exception e) {
                        Handler.ShowSnack("Houve um erro","MainActivity.GetByEAN.onResponse: " + e.getMessage(), MainActivity.this, R_ID);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Handler.ShowSnack("Houve um erro","MainActivity.GetByEAN.onFailure: " + t.toString(), MainActivity.this, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetByEAN: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void GetByDescription(String Description, RecyclerView recyclerView){
        try {
            Call<JsonObject> call = mainInterface.GetByDescription(4,SU.getSession(),SelectedShop,Description);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try{
                        if (!Handler.isRequestError(response,MainActivity.this,R_ID)) {
                            JsonObject jsonObject = response.body();
                            jsonArray = jsonObject.get("data").getAsJsonArray();
                            adapter = new MainAdapter(
                                jsonArray,MainActivity.this,R_ID
                            );
                            recyclerView.setAdapter(adapter);
                        }else{
                            DialogSearch.cancel();
                        }
                    }catch (Exception e) {
                        Handler.ShowSnack("Houve um erro","MainActivity.GetByDescription.onResponse: " + e.getMessage(),
                                MainActivity.this, R_ID);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Handler.ShowSnack("Houve um erro","MainActivity.GetByDescription.onFailure: " + t.toString(),
                            MainActivity.this, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetByDescription: " + e.getMessage(),
                    MainActivity.this, R_ID);
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
                Handler.ShowSnack("Houve um erro","MainActivity.onActivityResult: " + e.getMessage(), MainActivity.this, R_ID);
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

    private void GetApplicationAccessFunction(){
        try {
            Call<JsonObject> call = mainInterface.GetApplicationAccessFunction(4,SU.getSession(), SU.getId(),4);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!Handler.isRequestError(response,MainActivity.this,R_ID)){
                        try{
                            JsonObject jsonObject = response.body();
                            permissionsList = jsonObject.get("data").getAsJsonArray();
                            //GetShop();

                            ArrayList arrayList = new ArrayList<>();
                            arrayList.add("");

                            for(int i = 0; i < permissionsList.size(); i++){
                                JsonObject jsonPermission = permissionsList.get(i).getAsJsonObject();

                                if(jsonPermission.has("external_index") && jsonPermission.get("external_index") != JsonNull.INSTANCE){
                                    int id = jsonPermission.get("external_index").getAsInt();
                                    String description = jsonPermission.get("description").getAsString();

                                    arrayList.add(id + " - " + description);
                                }
                            }

                            ArrayAdapter<String> arrayAdapter= new ArrayAdapter<>(MainActivity.this,R.layout.support_simple_spinner_dropdown_item,arrayList);

                            spinner.setAdapter(arrayAdapter);
                        }catch (Exception e) {
                            Handler.ShowSnack("Houve um erro","MainActivity.GetApplicationAccessFunction.onResponse: " + e.getMessage(), MainActivity.this, R_ID);
                        }
                    }
                }

                @Override
                public void onFailure(@NotNull Call<JsonObject> call, Throwable t) {
                    Handler.ShowSnack("Houve um erro","MainActivity.GetApplicationAccessFunction.onFailure: " + t.toString(), MainActivity.this, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetApplicationAccessFunction: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void SetSpinner() {

        spinner.setOnTouchListener(spinnerOnTouch);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    Reset();
                    if(position!=0) {
                        SelectedShop = permissionsList.get(position-1).getAsJsonObject().get("external_index").getAsInt();
                        ((TextView) parent.getChildAt(0)).setTextColor(getApplicationContext().getColor(R.color.textColor));
                    }else{
                        SelectedShop = 0;
                    }
                } catch (Exception e) {
                    Handler.ShowSnack("Houve um erro", "MainActivity.SetSpinner.onItemSelected: " + e.getMessage(), MainActivity.this, R_ID);
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private final View.OnTouchListener spinnerOnTouch = (view, motionEvent) -> {
        GetApplicationAccessFunction();
        return false;
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void SetDialogSearch(){
        try {
            if(SelectedShop == 0){
                Handler.ShowSnack("Selecione uma loja",null,
                        MainActivity.this, R_ID);
                return;
            }
            DialogSearch = new Dialog(this,R.style.Theme_AppCompat_Dialog_MinWidth);
            DialogSearch.setContentView(R.layout.dialog_search);

            RecyclerView recyclerView = DialogSearch.findViewById(R.id.dialogSearch_RecyclerView);
            Button buttonOK = DialogSearch.findViewById(R.id.dialogSearch_ButtonOk);

            SearchView searchView = DialogSearch.findViewById(R.id.dialogSearch_SearchView);

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setHasFixedSize(true);

            SetRecyclerView(recyclerView);

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    try {
                        GetByDescription(s.toUpperCase(), recyclerView);
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","MainActivity.SetDialogSearch.onQueryTextChange: " + e.getMessage(),
                                MainActivity.this, R_ID);
                    }
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    return false;
                }
            });

            buttonOK.setOnClickListener(v ->{
                DialogSearch.cancel();
            });

            DialogSearch.create();
            DialogSearch.show();

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.SetDialogSearch: " + e.getMessage(),
                    this, R_ID);
        }
    }

    private void SetRecyclerView(RecyclerView recyclerView){
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {

            public void onItemClick(View view, int position) {
                try{
                    JsonObject jsonObject = adapter.getItem(position);
                    String price = "R$"+jsonObject.get("price").getAsString();
                    textViewPrice.setText(price);
                    textViewStore.setText(jsonObject.get("store").getAsString());
                    getSupportActionBar().setTitle(jsonObject.get("plu").getAsString());
                    getSupportActionBar().setSubtitle(jsonObject.get("description").getAsString());

                    SetColor(jsonObject);

                }catch (Exception e){
                    Handler.ShowSnack("Houve um erro","MainActivity.SetRecyclerView.onLongItemClick: " + e.getMessage(), MainActivity.this, R_ID);
                }

            }

            public boolean onLongItemClick(View view, final int position) {return false;}
        }));
    }

    private void SetColor(JsonObject jsonObject){
        if(jsonObject.get("promotion").getAsInt() == 1){
            textViewPrice.setTextColor(Color.RED);
        }else{
            textViewPrice.setTextColor(Color.WHITE);
        }

        if(jsonObject.get("store").getAsInt() < 1){
            textViewStore.setTextColor(Color.RED);
        }else{
            textViewStore.setTextColor(Color.WHITE);
        }
    }

}