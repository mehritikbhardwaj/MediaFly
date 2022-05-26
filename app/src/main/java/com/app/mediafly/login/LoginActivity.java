package com.app.mediafly.login;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.mediafly.MainActivity;
import com.app.mediafly.PortraitActivity;
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
    ProgressDialog pd;
    EditText et_email, et_password;
    LoginResponseModel model = new LoginResponseModel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btn_login = findViewById(R.id.btn_login);
        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);
        pd = new ProgressDialog(this);
        pd.setCancelable(false);
        et_email.setText(Utilities.getAndroidId(this));


        btn_login.setOnClickListener(view -> {
            if (et_email.getText().length() == 0 || et_password.getText().length() == 0) {
                Toast.makeText(this, "Please enter id and password.", Toast.LENGTH_SHORT).show();
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
        headers.put("deviceid", "0");

        LoginRequestModel requestModel = new LoginRequestModel();
        requestModel.setIp(Utilities.getIPAddress(true));
        requestModel.setDevice_serial("sample string 1");
        requestModel.setKey("AX7909");

        Call<LoginResponseModel> call = apiService.Login(headers, requestModel);

        call.enqueue(new Callback<LoginResponseModel>() {
            @Override
            public void onResponse(Call<LoginResponseModel> call, Response<LoginResponseModel> response) {
                if (pd.isShowing()) {
                    pd.dismiss();
                }

                if (response.isSuccessful()) {
                    model = response.body();

                    //   Toast.makeText(LoginActivity.this, model.getStore() + "", Toast.LENGTH_SHORT).show();


                    Utilities.setStringPreference(LoginActivity.this, Constants.IS_LOGGED_IN,
                            "YES", Constants.PREF_NAME);

                    Utilities.setStringPreference(LoginActivity.this, Constants.DEVICE_ID,
                            model.getDeviceid().toString(), Constants.PREF_NAME);


                    //Toast.makeText(LoginActivity.this, model.getDeviceid().toString(), Toast.LENGTH_SHORT).show();
                    Utilities.setStringPreference(LoginActivity.this, Constants.STORE_NAME,
                            model.getStore(), Constants.PREF_NAME);

                    Utilities.setStringPreference(LoginActivity.this, Constants.COMPANY_NAME,
                            model.getCompany(), Constants.PREF_NAME);

                    Utilities.setStringPreference(LoginActivity.this, Constants.ORIENTATION,
                            model.getOrientation(), Constants.PREF_NAME);

                    if (Utilities.getStringPref(LoginActivity.this, Constants.ORIENTATION, Constants.PREF_NAME).equals("Portrait")) {
                        Intent intent = new Intent(getApplicationContext(), PortraitActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
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
