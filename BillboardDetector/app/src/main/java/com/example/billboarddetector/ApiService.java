// ApiService.java
package com.example.billboarddetector;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("analyze") // This should be the endpoint name on your backend
    Call<AnalysisResult> uploadBillboard(
            @Part MultipartBody.Part image,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude
    );
}