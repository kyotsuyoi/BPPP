package com.bppp.Main;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bppp.R;
import com.google.gson.JsonArray;
import com.bppp.CommonClasses.Handler;
import com.google.gson.JsonObject;

public class MainAdapter extends RecyclerView.Adapter <MainAdapter.ViewHolder> {

    private JsonArray list;
    private Activity activity;
    private com.bppp.CommonClasses.Handler Handler = new Handler();
    private int R_ID;

    public MainAdapter(JsonArray list, Activity activity, int R_ID){
        this.list = list;
        this.activity = activity;
        this.R_ID = R_ID;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try{
            JsonObject jsonObject = list.get(position).getAsJsonObject();
            //int ID =  jsonObject.get("plu").getAsInt();
            //String Status =  jsonObject.get("status").getAsString() ;
            holder.textViewPLU.setText(jsonObject.get("plu").getAsString());
            holder.textViewDescription.setText(jsonObject.get("description").getAsString());

            if(jsonObject.get("promotion").getAsInt() == 1){
                holder.textViewPLU.setTextColor(Color.RED);
            }else{
                holder.textViewPLU.setTextColor(Color.WHITE);
            }

            if(jsonObject.get("status").getAsString().equalsIgnoreCase("I")){
                holder.textViewDescription.setPaintFlags(holder.textViewDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }else{
                holder.textViewDescription.setPaintFlags(Paint.LINEAR_TEXT_FLAG);
            }
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro", "MainAdapter.onBindViewHolder: " + e.getMessage(), activity, R_ID);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public JsonObject getItem(int position){
        return list.get(position).getAsJsonObject();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textViewPLU, textViewDescription;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewPLU = itemView.findViewById(R.id.item_TextViewPLU);
            textViewDescription = itemView.findViewById(R.id.item_TextViewDescription);
        }
    }
}
