package com.bppp.Login;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.bppp.CommonClasses.ApiClient;
import com.bppp.CommonClasses.Handler;
import com.bppp.CommonClasses.SavedUser;
import com.bppp.Main.MainActivity;
import com.bppp.R;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    private static final int REQUEST_STORAGE = 0;
    private static final String BPPP_PREFERENCES = "BPPP_PREFERENCES";

    private SavedUser SU = new SavedUser();
    private AutoCompleteTextView autoCompleteTextViewUser;
    private View mLoginFormView;
    private EditText editTextPassword;
    private TextView textViewInfo;
    private CheckBox check;
    private boolean saved;
    private com.bppp.CommonClasses.Handler Handler = new Handler();
    private int R_ID = R.id.login_form;
    private Activity This = LoginActivity.this;

    private LoginInterface loginInterface = ApiClient.getApiClient().create(LoginInterface.class);;
    private JsonObject systemVersion;
    private JsonObject login;
    private Call<JsonObject> systemVersionCall;
    private Call<JsonObject> loginCall;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        autoCompleteTextViewUser = findViewById(R.id.activityLogin_AutoCompleteTextView_User);
        populateAutoComplete();

        Objects.requireNonNull(getSupportActionBar()).hide();

        editTextPassword = findViewById(R.id.activityLogin_EditText_Password);
        editTextPassword.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        mLoginFormView = findViewById(R.id.login_form);
        //progressBar = findViewById(R.id.login_progress);
        check = findViewById(R.id.login_check);
        textViewInfo = findViewById(R.id.activityLogin_TextViewInfo);

        findViewById(R.id.activityLogin_Button_Password).setOnClickListener(v->{
            if(!isLoginValid(autoCompleteTextViewUser.getText().toString())){
                autoCompleteTextViewUser.setError("Usuário inválido");
                Handler.ShowSnack("Insira nome de usuário e senha antiga para cadastrar a nova senha",null,this,R_ID);
                return;
            }
            if(!isPasswordValid(editTextPassword.getText().toString())){
                editTextPassword.setError("Senha inválida");
                Handler.ShowSnack("Insira nome de usuário e senha antiga para cadastrar a nova senha",null,this,R_ID);
                return;
            }
            DialogPassword();
        });

        //Hide keyboard during check click
        check.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        });

        LoadSavedSettings();

        saved=false;

        int i= SU.getId();

        if (i!=0){
            saved = true;
        }

        GetSystemVersion();
    }

    protected void onRestart() {
        super.onRestart();
    }

    protected void onStop() {
        super.onStop();
        if(loginCall != null) {
            loginCall.cancel();
        }
        if(systemVersionCall != null) {
            systemVersionCall.cancel();
        }
    }

    public void OnLoginClick(View view){
        //Hide keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        saved=false;
        attemptLogin();
    }

    private void populateAutoComplete() {
        try {
            if (!mayRequestStorage()) {
                return;
            }
            CreateFolders();
        }catch (Exception e){
            Handler.ShowSnack(
                    "Houve um erro",
                    "LoginActivity.populateAutoComplete: "+e.getMessage(),
                    This,
                    R_ID
            );
        }
    }

    private void CreateFolders() {
        File PicturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        assert PicturesDir != null;
        if(!PicturesDir.exists()){
            PicturesDir.getParentFile().mkdirs();
        }
        File DocumentsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        assert DocumentsDir != null;
        if(!DocumentsDir.exists()){
            DocumentsDir.getParentFile().mkdirs();
        }
    }

    private boolean mayRequestStorage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {
            Snackbar.make(autoCompleteTextViewUser, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE).setAction(
                    android.R.string.ok, v -> requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, REQUEST_STORAGE));
        } else {
            requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, REQUEST_STORAGE);
        }
        return false;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    private void attemptLogin() {

        // Reset errors.
        autoCompleteTextViewUser.setError(null);
        editTextPassword.setError(null);

        // Store values at the time of the login attempt.
        String user = autoCompleteTextViewUser.getText().toString().toLowerCase();
        String password = editTextPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError(getString(R.string.error_field_required));
            focusView = editTextPassword;
            cancel = true;
        }else if(!isPasswordValid(password)){
            editTextPassword.setError(getString(R.string.error_invalid_password));
            focusView = editTextPassword;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(user)) {
            autoCompleteTextViewUser.setError(getString(R.string.error_field_required));
            focusView = autoCompleteTextViewUser;
            cancel = true;
        } else if (!isLoginValid(user)) {
            autoCompleteTextViewUser.setError(getString(R.string.error_invalid_user));
            focusView = autoCompleteTextViewUser;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.

            int vSaved = 0;
            if (saved) {
                vSaved = 1;
            }
            ShowLoginForm(false);
            PostLogin(user, password);
        }
    }

    private boolean isLoginValid(String user) {
        return user.length() >2;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 3;
    }

    private void ShowLoginForm(final boolean show) {

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        if(show) {
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(1);
            mLoginFormView.setVisibility(View.VISIBLE);
        }else {
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(0);
            mLoginFormView.setVisibility(View.GONE);
        }
    }

    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI, ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,
                ContactsContract.Contacts.Data.MIMETYPE + " = ?", new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE},
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC"
        );
    }

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> login = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            login.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }
        addLoginToAutoComplete(login);
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) { }

    private void addLoginToAutoComplete(List<String> list) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(LoginActivity.this, android.R.layout.simple_dropdown_item_1line, list);
        autoCompleteTextViewUser.setAdapter(adapter);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Email.IS_PRIMARY,};
        int ADDRESS = 0;
        //int IS_PRIMARY = 1;
    }

    private void Login(int ID, String User, String Password, String Session, boolean Administrator){
        try {

            SU.setId(ID);
            SU.setUser(User);
            SU.setPassword(Password);
            SU.setSession(Session);
            SU.setAdministrator(Administrator);

            SavedUser.setSavedUser(SU);

            if(SU!= null){
                if(check.isChecked()) {
                    SharedPreferences settings = getSharedPreferences(BPPP_PREFERENCES, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt("id", SU.getId());
                    editor.putString("user", SU.getUser());
                    editor.putString("password", SU.getPassword());
                    editor.putString("session",SU.getSession());
                    editor.putBoolean("administrator",SU.isAdministrator());
                    editor.apply();

                    //Handler.ShowSnack("Login OK: O usuário foi salvo localmente", This, R_ID, true);
                }

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                //intent.putExtra("Check",check.isChecked());

                startActivity(intent);
                finish();
            }else{
                Handler.ShowSnack(
                        "Falha na autenticação",
                        "Não foi possível autenticar o usuário "+SU.getUser()+" que estava salvo localmente",
                        This,
                        R_ID
                );
            }

        } catch (Exception e) {
            Handler.ShowSnack(
                    "Houve um erro",
                    "LoginActivity.Login: "+e.getMessage(),
                    This,
                    R_ID
            );
        }
        ShowLoginForm(true);
    }

    private void LoadSavedSettings() {
        try {
            SharedPreferences settings = getSharedPreferences(BPPP_PREFERENCES, 0);
            SU.setId(settings.getInt("id", 0));
            SU.setUser(settings.getString("user", ""));
            SU.setPassword(settings.getString("password", ""));
            SU.setSession(settings.getString("session", ""));
            SU.setAdministrator(settings.getBoolean("administrator", false));
            SU.setAdminVisualization(settings.getBoolean("adminVisualization",false));
            SavedUser.setSavedUser(SU);
        }catch (Exception e){
            Handler.ShowSnack(
                    "Houve um erro",
                    "LoginActivity.LoadSavedSettings: "+e.getMessage(),
                    This,
                    R_ID
            );
        }
    }

    private void GetSystemVersion(){
        try {
            ShowLoginForm(false);
            systemVersionCall = loginInterface.GetAppVersion(4);
            Handler.ShowSnack("Aguarde por favor...",null,This,R_ID);
            systemVersionCall.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try {
                        systemVersion = response.body();

                        if (!Handler.isRequestError(response, This, R_ID)) {
                            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            String version = packageInfo.versionName;

                            JsonArray data = systemVersion.get("data").getAsJsonArray();
                            String SysName = data.get(0).getAsJsonObject().get("description").getAsString();
                            String SysVer = data.get(0).getAsJsonObject().get("version").getAsString();
                            textViewInfo.setText(String.format("%s ver.%s Created by Kyo", SysName, SysVer));

                            if (!version.equalsIgnoreCase(SysVer)) {
                                final android.app.AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                builder.setCancelable(false);
                                builder.setMessage("Aplicação desatualizada, a versão necessária é " + SysVer + ", a versão atual é " + version);
                                builder.setPositiveButton("OK", (dialogInterface, i) -> finish());
                                builder.show();
                            } else {
                                if (saved) {
                                    check.setChecked(true);
                                    PostLogin(SU.getUser(),SU.getPassword());
                                }else{
                                    ShowLoginForm(true);
                                    Handler.ShowSnack("Entre com login e senha",null,This,R_ID);
                                }
                            }
                        }

                    } catch (Exception e) {
                        Handler.ShowSnack(
                                "Houve um erro",
                                "LoginActivity.SelectSystemVersion.onResponse: "+e.toString(),
                                This,
                                R_ID
                        );
                        ShowLoginForm(true);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Handler.ShowSnack(
                            "Houve um erro",
                            "LoginActivity.SelectSystemVersion.onFailure: "+t.toString(),
                            This,
                            R_ID
                    );
                }
            });

        }catch (Exception e){
            Handler.ShowSnack(
                    "Houve um erro",
                    "LoginActivity.SelectSystemVersion: "+e.getMessage(),
                    This,
                    R_ID
            );
        }
    }

    private void PostLogin(String user, String password) {
        try {
            ShowLoginForm(false);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("user",user);
            jsonObject.addProperty("password",password);
            loginCall = loginInterface.PostLogin(4,jsonObject);

            loginCall.enqueue(new Callback<JsonObject>() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try {
                        if (!Handler.isRequestError(response, This, R_ID)) {
                            login = response.body();
                            JsonObject data = login.get("data").getAsJsonObject();
                            int id = data.get("id").getAsInt();
                            String session = data.get("session").getAsString();
                            int administrator = data.get("administrator").getAsInt();
                            boolean isAdministrator;
                            if (administrator == 0) {
                                isAdministrator = false;
                            } else {
                                isAdministrator = true;
                            }
                            Login(id, user, password, session, isAdministrator);
                        } else {
                            /*JsonObject jsonError = new JsonParser().parse(response.errorBody().toString()).getAsJsonObject();
                            String message = jsonError.get("message").getAsString();
                            if(message.contains("Default password is not permited")){
                                DialogPassword();
                            }*/
                            ShowLoginForm(true);
                        }
                    }catch (Exception e){
                        Handler.ShowSnack(
                                "Houve um erro",
                                "LoginActivity.PostLogin.onResponse: "+e.toString(),
                                This,
                                R_ID
                        );
                        ShowLoginForm(true);
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Handler.ShowSnack(
                            "Houve um erro",
                            "LoginActivity.SelectLogin.onFailure: "+t.toString(),
                            This,
                            R_ID
                    );
                    ShowLoginForm(true);
                }
            });
        }catch (Exception e){
            Handler.ShowSnack(
                    "Houve um erro",
                    "LoginActivity.SelectLogin: "+e.getMessage(),
                    This,
                    R_ID
            );
            ShowLoginForm(true);
        }
    }

    private void DialogPassword(){
        final android.app.Dialog dialog = new Dialog(this, R.style.Theme_AppCompat_Dialog_MinWidth);
        dialog.setContentView(R.layout.dialog_password);
        EditText editTextNewPassword = dialog.findViewById(R.id.dialogPassword_EditText_Password);
        EditText editTextNewPasswordConfirm = dialog.findViewById(R.id.dialogPassword_EditText_PasswordConfirm);
        Button button = dialog.findViewById(R.id.dialogPassword_Button_Confirm);

        button.setOnClickListener(view -> {
            if(editTextNewPassword.length() < 8){
                editTextNewPassword.setError("A senha precisa ter mais de 7 caracteres");
                return;
            }
            if(editTextNewPasswordConfirm.length() < 8){
                editTextNewPasswordConfirm.setError("A senha precisa ter mais de 7 caracteres");
                return;
            }

            if(!editTextNewPassword.getText().toString().equals(editTextNewPasswordConfirm.getText().toString())){
                editTextNewPasswordConfirm.setError("A senha não coinside");
                return;
            }

            PutPassword(autoCompleteTextViewUser.getText().toString(),editTextPassword.getText().toString(),editTextNewPassword.getText().toString());
            dialog.cancel();
        });

        dialog.show();
    }

    private void PutPassword(String user, String password, String newPassword) {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("user",user);
            jsonObject.addProperty("password",password);
            jsonObject.addProperty("new_password",newPassword);
            loginCall = loginInterface.PutLogin(4,jsonObject);

            loginCall.enqueue(new Callback<JsonObject>() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    try {
                        if (!Handler.isRequestError(response, This, R_ID)) {
                            editTextPassword.setText("");
                            Handler.ShowSnack("Senha alterada, faça login normalmente",null,LoginActivity.this,R_ID);
                        }
                    }catch (Exception e){
                        Handler.ShowSnack(
                                "Houve um erro",
                                "LoginActivity.PutPassword.onResponse: "+e.toString(),
                                This,
                                R_ID
                        );
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Handler.ShowSnack(
                            "Houve um erro",
                            "LoginActivity.PutPassword.onFailure: "+t.toString(),
                            This,
                            R_ID
                    );
                }
            });
        }catch (Exception e){
            Handler.ShowSnack(
                    "Houve um erro",
                    "LoginActivity.PutPassword: "+e.getMessage(),
                    This,
                    R_ID
            );
        }
    }

}