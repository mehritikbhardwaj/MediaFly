package com.app.mediafly;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.app.mediafly.common.Constants;
import com.app.mediafly.common.Utilities;
import com.app.mediafly.common.VerticalTextView;
import com.app.mediafly.database.mediaDatabase;
import com.app.mediafly.login.LoginActivity;
import com.app.mediafly.retrofit.ApiService;
import com.app.mediafly.retrofit.RetroClient;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    ProgressDialog mProgressDialog;
    VideoView videoView;
    mediaDatabase mediaDb;

    List<NewsModel> newsList = new ArrayList<>();
    List<MediaModel> mediaList = new ArrayList<>();

    ArrayList<NewsModel> newsItemList = new ArrayList<>();
    ArrayList<String> fileName = new ArrayList<>();
    ArrayList<String> fileType = new ArrayList<>();
    ArrayList<String> qrUrl = new ArrayList<>();
    ArrayList<String> pendingDownloads = new ArrayList<>();

    VerticalTextView headingText, newsText;
    Integer j = 0, i = 0, downloadedCount = 0, availableForDownload = 0;
    ImageView imageQR, imageView;
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 5000;

    @Override
    protected void onResume() {
        callGetNewsListApi();
        handler.postDelayed(runnable = () -> {
            handler.postDelayed(runnable, delay);
            setText();
        }, delay);

        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.landscape_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        declareUiThings();

        mediaDb = new mediaDatabase(this);

        // callGetNewsListApi();
        //  mediaDb.clearDatabase();

        if (mediaDb.checkDbIsEmpty()) {
            playFromRaw();
            callGetMediaListApi();
        } else {
            getDataFromDbAndPlayMedia();
        }

        videoView.setOnCompletionListener(mediaPlayer -> {
            if (mediaDb.checkDbIsEmpty()) {
                playFromRaw();
                callGetMediaListApi();
            } else {
                if (availableForDownload.equals(downloadedCount)) {
                    getDataFromDbAndPlayMedia();
                } else {
                    playFromRaw();
                }
            }
        });

    }

    private void getDataFromDbAndPlayMedia() {
        fileType = mediaDb.getDownloadedFileList("format");
        fileName = mediaDb.getDownloadedFileList("fileName");
        pendingDownloads = mediaDb.getPendingFileNames();

        if (!pendingDownloads.isEmpty()) {
            callDownloadMediaFunction(pendingDownloads.get(0), true);
        }
        Toast.makeText(this, String.valueOf(pendingDownloads.size()), Toast.LENGTH_SHORT).show();
        playGraphics();
    }

    private void declareUiThings() {
        videoView = findViewById(R.id.videoView);
        headingText = findViewById(R.id.headingText);
        newsText = findViewById(R.id.newsText);
        imageQR = findViewById(R.id.imageQR);
        imageView = findViewById(R.id.imageView);
    }

    //Play video and images from the available data
    private void playGraphics() {
        handler = new Handler();
        Integer cnt = fileType.size();
        if (i < cnt) {
        } else {
            i = 0;
        }
        String type = fileType.get(i);

        String path = fileName.get(i);
        i++;
        if (type.equals("Video")) {
            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            try {
                videoView.setVideoURI(Uri.parse(Environment.getExternalStorageDirectory()
                        + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + path));
                videoView.start();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            videoView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            try {
                Uri uri = Uri.parse(Environment.getExternalStorageDirectory()
                        + File.separator + Environment.DIRECTORY_DOWNLOADS +
                        File.separator + path);
                imageView.setImageURI(uri);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            handler.postDelayed(() -> {
                playGraphics();
            }, 10000);
        }
        // generateQR(i);
    }

    private void generateQR() {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode("https://www.google.com",
                    BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            imageQR.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void playFromRaw() {
        String path = "android.resource://" + getPackageName() + "/" + R.raw.mediafly;
        videoView.setVideoURI(Uri.parse(path));
        videoView.start();
    }

    private void callDownloadMediaFunction(String fileName, Boolean isComingFromPending) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Downloading Resources");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        // execute this when the downloader must be fired
        final DownloadTask downloadTask = new DownloadTask(this, fileName, isComingFromPending);
        downloadTask.execute(Constants.BASE_URL + fileName);

        mProgressDialog.setOnCancelListener(dialog -> {
            downloadTask.cancel(true); //cancel the task
        });
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private final Context context;
        private String fileName = "";
        private final Boolean isComingFromPending;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context, String fileName, Boolean isComingFromPending) {
            this.context = context;
            this.fileName = fileName;
            this.isComingFromPending = isComingFromPending;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                String path = Environment.getExternalStorageDirectory() +
                        File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + fileName;
                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(path);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) MainActivity.this.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (availableForDownload > downloadedCount) {
                mediaDb.updateData(fileName);
                callDownloadMediaFunction(MainActivity.this.fileName.get(downloadedCount), false);
                downloadedCount++;
            }
            if (availableForDownload.equals(downloadedCount)) {
                playGraphics();
            }
            if (isComingFromPending) {
                mediaDb.updateData(fileName);
            }
            if (result != null) {
                Toast.makeText(MainActivity.this, "Download error: " + result, Toast.LENGTH_LONG).show();
                Log.d("Download", result);
            } else
                Toast.makeText(MainActivity.this, "File downloaded", Toast.LENGTH_SHORT).show();
        }
    }


    private void callGetNewsListApi() {

        ApiService apiService = RetroClient.getApiService();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("apiusername", Constants.API_USER_NAME);
        headers.put("apipassword", Constants.API_PASSWORD);
        headers.put("uid", "0");
        headers.put("scode", "0");
        headers.put("deviceid", "1");

        Call<List<NewsModel>> call = apiService.GetNews(headers, Utilities.getIPAddress(true));

        call.enqueue(new Callback<List<NewsModel>>() {
            @Override
            public void onResponse(Call<List<NewsModel>> call, Response<List<NewsModel>> response) {

                if (response.isSuccessful()) {
                    newsList = response.body();
                    if (!newsList.isEmpty()) {

                        for (int i = 0; i < newsList.size(); i++) {
                            /*Boolean insertResponse =*/
                            newsItemList.add(newsList.get(i));
                        }
                        setText();
                        generateQR();
                    } else {
                        Toast.makeText(MainActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<NewsModel>> call, Throwable t) {
                Log.e("ONFAILURE", t.toString());
            }
        });
    }


    private void callAppInfoApi() {
        ApiService apiService = RetroClient.getApiService();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("apiusername", Constants.API_USER_NAME);
        headers.put("apipassword", Constants.API_PASSWORD);
        headers.put("uid", "0");
        headers.put("scode", "0");
        headers.put("deviceid", Utilities.getStringPref(this, Constants.DEVICE_ID, Constants.PREF_NAME));


        Call<AppInfoModel> call = apiService.AppInfoAndValidate(headers, Utilities.getAndroidId(this), Utilities.getIPAddress(true));

        call.enqueue(new Callback<AppInfoModel>() {
            @Override
            public void onResponse(Call<AppInfoModel> call, Response<AppInfoModel> response) {

                if (response.isSuccessful()) {
                    AppInfoModel model = response.body();

                    if (model.getIsValidDevice() == 0) {
                        Utilities.setStringPreference(MainActivity.this, Constants.IS_LOGGED_IN,
                                "NO", Constants.PREF_NAME);

                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        if (model.getVersion().toString().equals(Utilities.getStringPref(
                                MainActivity.this, Constants.APP_VERSION, Constants.PREF_NAME))) {
                            Toast.makeText(MainActivity.this, "Update Available", Toast.LENGTH_SHORT).show();
                        }
                    }

                } else {
                    Toast.makeText(MainActivity.this,
                            "Something went wrong!",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AppInfoModel> call, Throwable t) {
                Log.e("ONFAILURE", t.toString());
            }
        });


    }

    private void callGetMediaListApi() {
        ApiService apiService = RetroClient.getApiService();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("apiusername", Constants.API_USER_NAME);
        headers.put("apipassword", Constants.API_PASSWORD);
        headers.put("uid", "0");
        headers.put("scode", "0");
        headers.put("deviceid", "1");

        //  headers.put("deviceid", Utilities.getStringPref(this, Constants.DEVICE_ID, Constants.PREF_NAME));


        Call<List<MediaModel>> call = apiService.GetMedia(headers, Utilities.getIPAddress(true));

        call.enqueue(new Callback<List<MediaModel>>() {
            @Override
            public void onResponse(Call<List<MediaModel>> call, Response<List<MediaModel>> response) {
                if (response.isSuccessful()) {
                    mediaList = response.body();
                    if (!mediaList.isEmpty()) {

                        for (int i = 0; i < mediaList.size(); i++) {

                            Boolean insertResponse = mediaDb.insert_data(
                                    (mediaList.get(i).getSize()),
                                    (mediaList.get(i).getFormat()),
                                    (mediaList.get(i).getFilename()),
                                    (mediaList.get(i).getStime()),
                                    (mediaList.get(i).getEtime()),
                                    (mediaList.get(i).getOrder()),
                                    (mediaList.get(i).getEid()),
                                    0,
                                    (mediaList.get(i).getQrcode()),
                                    (mediaList.get(i).getSdate()),
                                    (mediaList.get(i).getEdate()),mediaList.get(i).getDuration());

                            if (insertResponse) {
                                fileName.add((mediaList.get(i).getFilename()));
                                fileType.add((mediaList.get(i).getFormat()));
                                qrUrl.add(mediaList.get(i).getQrcode());
                            }
                        }
                        availableForDownload = fileName.size();
                        callDownloadMediaFunction(fileName.get(downloadedCount), false);
                    } else {
                        Toast.makeText(MainActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<MediaModel>> call, Throwable t) {
                Log.e("ONFAILURE", t.toString());
                Toast.makeText(MainActivity.this, t + " Something went wrong!", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void generateQR(Integer i) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(qrUrl.get(i),
                    BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            imageQR.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void setText() {
        if (j <= 4) {
            headingText.setText(newsItemList.get(j).getTitle());
            newsText.setText(newsItemList.get(j).getDescription());
            j++;
        } /*else if (j < 7) {
            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.0f
            );
            videoView.setLayoutParams(param);
            j++;*/ else {
          /*  LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0.9f
            );
            videoView.setLayoutParams(param);


            LinearLayout.LayoutParams params = new
                    LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0.9f);
            params.weight = 0.9f;
            params.gravity = Gravity.CENTER;
            videoView.setLayoutParams(params);*/
            j = 0;
        }
    }


}
