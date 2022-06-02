package com.app.mediafly.retrofit;

import com.app.mediafly.AppInfoModel;
import com.app.mediafly.MediaModel;
import com.app.mediafly.NewsModel;
import com.app.mediafly.common.SuccessModel;
import com.app.mediafly.login.LoginRequestModel;
import com.app.mediafly.login.LoginResponseModel;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Query;


public interface ApiService {

    @POST("Login")
    Call<LoginResponseModel> Login(@HeaderMap Map<String, String> headers,
                                   @Body LoginRequestModel requestModel);

    @GET("AppInfoAndValidate")
    Call<AppInfoModel> AppInfoAndValidate(@HeaderMap Map<String, String> headers,
                                          @Query("device_serial") String device_serial,
                                          @Query("ip") String ip);

    @GET("GetMedia")
    Call<List<MediaModel>> GetMedia(@HeaderMap Map<String, String> headers,
                                    @Query("ip") String ip);

    @GET("MediaPlay")
    Call<SuccessModel> MediaPlay(@HeaderMap Map<String, String> headers,
                                 @Query("mid") String mid);


    @GET("CheckShowNewsStatus")
    Call<SuccessModel> CheckShowNewsStatus(@HeaderMap Map<String, String> headers);

    @GET("GetNews")
    Call<List<NewsModel>> GetNews(@HeaderMap Map<String, String> headers,
                                  @Query("ip") String ip);

}
