package com.bppp.Main;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface MainInterface {

    @GET("BPPP/Product.php")
    Call<JsonObject> GetByID(
            @Query("app_id") int app_id,
            @Query("AUTH") String auth,
            @Query("shop_id") int shop_id,
            @Query("id") String plu
    );

    @GET("BPPP/Product.php")
    Call<JsonObject> GetByEAN(
            @Query("app_id") int app_id,
            @Query("AUTH") String auth,
            @Query("shop_id") int shop_id,
            @Query("ean") String ean
    );

    @GET("BPPP/Product.php")
    Call<JsonObject> GetByDescription(
            @Query("app_id") int app_id,
            @Query("AUTH") String auth,
            @Query("shop_id") int shop_id,
            @Query("description") String description
    );

    /*@GET("BPPP/Shop.php")
    Call<JsonObject> GetShop(
            @Query("app_id") int app_id,
            @Query("AUTH") String auth,
            @Query("company_id") int company_id
    );*/

    @GET("CCPP/ApplicationAccessFunction.php")
    Call<JsonObject> GetApplicationAccessFunction(
            @Query("app_id") int app_id,
            @Query("AUTH") String auth,
            @Query("user_id") int user_id,
            @Query("application_id") int application_id
    );
}
