// RetrofitClient.java
package com.example.billboarddetector;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // IMPORTANT: REPLACE THIS WITH YOUR FRIEND'S ACTUAL BACKEND URL
    private static final String BASE_URL ="https://391df55bf6b6.ngrok-free.app/"; // Example: "http://[YOUR_IP_ADDRESS]:[PORT]/"

    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}