package com.bppp.CommonClasses;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.bppp.R;

import retrofit2.Response;

public class Handler {

    public static int SelectedTaskID; //Store the task that user is choose
    public static boolean isLogged = false;

    public boolean isRequestError(Response<JsonObject> response, Activity activity, int R_ID){
        try {
            JsonObject jsonError;
            int code = response.code();
            switch (code) {
                case 200:
                    return isMessageError(response.body(), activity, R_ID);
                case 400:
                    jsonError = new JsonParser().parse(response.errorBody().string()).getAsJsonObject();
                    return  isMessageError(jsonError, activity, R_ID);
                case 401:
                    jsonError = new JsonParser().parse(response.errorBody().string()).getAsJsonObject();
                    return isMessageError(jsonError, activity, R_ID);
                case 404:
                    ShowSnack("Caminho não encontrado",response.raw().toString(), activity, R_ID, true);
                    return true;
                default:
                    ShowSnack(response.message(),response.raw().toString(), activity, R_ID,true);
                    return true;
            }
        }catch (Exception e){
            ShowSnack("Houve um erro","Handler.isRequestError: \n"+e.getMessage(), activity, R_ID,true);
            return true;
        }
    }

    public boolean isMessageError(JsonObject jsonObject, Activity activity, int R_ID){
        try {
            if(jsonObject.get("error").getAsBoolean()) {
                String message = jsonObject.get("message").getAsString();

                if (message.contains("No data")){
                    ShowSnack("Nada encontrado", null, activity, R_ID, false);
                }else if(message.contains("Error on delete data")) {
                    ShowSnack("Esta tarefa não pode ser removida",
                            "Esta tarefa contém informações vinculadas como por exemplo mensagens, usuários, listas, etc\n\n" + message,
                            activity,
                            R_ID,
                            true
                    );
                }else if (message.contains("This user is blocked")){
                    ShowSnack("Usuário bloqueado", null, activity, R_ID, false);
                }else if (message.contains("This user does not have access to this application")){
                    ShowSnack("Este usuário não tem acesso nesta aplicação", null, activity, R_ID, false);
                }else if (message.contains("blocked state_id")){
                    ShowSnack("O estado dessa tarefa mudou", "Volte e atualize sua lista, esta tarefa mudou seu estado", activity, R_ID, true);
                }else if (message.contains("Authorization denied")){
                    ShowSnack("Acesso negado", "Sua sessão expirou.\nTalvez tenha sido conectado em outro dispositivo", activity, R_ID, true);
                }else {
                    ShowSnack("Houve um erro", jsonObject.get("message").getAsString(), activity, R_ID, true);
                }
            }else{
                return false;
            }
        }catch (Exception e){
            ShowSnack("Houve um erro","Handler.isMessageError: \n"+e.getMessage(), activity, R_ID,true);
        }
        return true;
    }

    public void ShowSnack(String message, String fullMessage, Activity activity, int R_ID, boolean isError){
        try {
            if (isError){
                View.OnClickListener mOnClickListener;
                mOnClickListener = v -> {

                    final Dialog dialog = new Dialog(activity, R.style.Theme_AppCompat_Dialog_MinWidth);
                    dialog.setContentView(R.layout.dialog_full_message);
                    TextView textView = dialog.findViewById(R.id.dialogFullMessage_textView);
                    Button button = dialog.findViewById(R.id.dialogFullMessage_button);

                    button.setOnClickListener(view -> {
                        dialog.cancel();
                    });

                    textView.setText(fullMessage);
                    dialog.show();
                };
                Snackbar.make(activity.findViewById(R_ID),message,5000).setAction("Ver",mOnClickListener).setActionTextColor(Color.RED).show();
            }else{
                Snackbar.make(activity.findViewById(R_ID), message, 5000).show();
            }
        }catch (Exception e){
            Toast.makeText(activity.getApplicationContext(), "Handler.ShowSnack: \n"+e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
