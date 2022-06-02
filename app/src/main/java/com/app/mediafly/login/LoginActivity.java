package com.app.mediafly.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.mediafly.MainActivity;
import com.app.mediafly.PortraitTvActivity;
import com.app.mediafly.R;
import com.app.mediafly.common.Constants;
import com.app.mediafly.common.Utilities;
import com.app.mediafly.common.Utils;
import com.app.mediafly.retrofit.ApiService;
import com.app.mediafly.retrofit.RetroClient;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    Button btn_login;
    Context mContext;
    ProgressDialog pd;
    EditText et_email, et_password;
    LoginResponseModel model = new LoginResponseModel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mContext = this;
        btn_login = findViewById(R.id.btn_login);
        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);
        pd = new ProgressDialog(this);
        pd.setCancelable(false);


        btn_login.setOnClickListener(view -> {
            if (et_password.getText().length() == 0) {
                Toast.makeText(this, "Please enterpassword.", Toast.LENGTH_SHORT).show();
            } else {
                if (Utils.isNetworkAvailable(this)) {
                    callLoginApi();
                } else {
                    Toast.makeText(this, R.string.check_internet, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void callLoginApi() {
        pd.setMessage("Please Wait...");
        pd.show();
        ApiService apiService = RetroClient.getApiService();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("apiusername", Constants.API_USER_NAME);
        headers.put("apipassword", Constants.API_PASSWORD);
        headers.put("uid", "0");
        headers.put("scode", "0");
        headers.put("deviceid", Utilities.getStringPref(this, Constants.DEVICE_ID, Constants.PREF_NAME));

        LoginRequestModel requestModel = new LoginRequestModel();
        requestModel.setIp(Utilities.getIPAddress(true));
        requestModel.setDevice_serial( Utilities.getAndroidId(this));
        requestModel.setKey(et_password.getText().toString());

        Call<LoginResponseModel> call = apiService.Login(headers, requestModel);

        call.enqueue(new Callback<LoginResponseModel>() {
            @Override
            public void onResponse(Call<LoginResponseModel> call, Response<LoginResponseModel> response) {
                if (pd.isShowing()) {
                    pd.dismiss();
                }

                if (response.isSuccessful()) {
                    model = response.body();

                    Utilities.setStringPreference(mContext, Constants.IS_LOGGED_IN,
                            "YES", Constants.PREF_NAME);


                    Utilities.setStringPreference(mContext, Constants.ORIENTATION, model.getOrientation(),
                            Constants.PREF_NAME);

                    Utilities.setStringPreference(mContext, Constants.DEVICE_ID,
                            String.valueOf(model.getDeviceid()), Constants.PREF_NAME);




                    String DeviceId = String.valueOf(model.getDeviceid());

                    if (Utilities.getStringPref(mContext, Constants.ORIENTATION, Constants.PREF_NAME).equals("Portrait")) {
                        Intent intent = new Intent(getApplicationContext(), PortraitTvActivity.class);
                        intent.putExtra("deviceid",DeviceId);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                        intent1.putExtra("deviceid",DeviceId);
                        startActivity(intent1);
                        finish();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid login credentials!",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponseModel> call, Throwable t) {
                if (pd.isShowing()) {
                    pd.dismiss();
                }
                Log.e("ONFAILURE", t.toString());
            }
        });

    }
}
