package com.app.mediafly;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
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
import com.app.mediafly.common.SuccessModel;
import com.app.mediafly.common.Utilities;
import com.app.mediafly.common.Utils;
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

public class PortraitActivity extends AppCompatActivity {

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
    int delay = 1000 * 10;
    int count = 0;

    @Override
    protected void onResume() {
        handler.postDelayed(runnable = () -> {
            handler.postDelayed(runnable, delay);
            setText();
        }, delay);
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        declareUiThings();

        mediaDb = new mediaDatabase(this);
        if (Utils.isNetworkAvailable(this)) {
            callGetNewsListApi();
        } else {
            Toast.makeText(this, R.string.check_internet, Toast.LENGTH_SHORT).show();
        }
        // mediaDb.clearDatabase();

        // callGetMediaListApi();

        handleTextViewVisibility();
        if (mediaDb.checkDbIsEmpty()) {
            playFromRaw();
            generateQR();
            if (Utils.isNetworkAvailable(this)) {
                callGetMediaListApi();
            } else {
                Toast.makeText(this, R.string.check_internet, Toast.LENGTH_SHORT).show();
            }
        } else {
            getDataFromDbAndPlayMedia();
        }

        videoView.setOnCompletionListener(mediaPlayer -> {
            if (mediaDb.checkDbIsEmpty()) {
                playFromRaw();
                if (Utils.isNetworkAvailable(this)) {
                    callGetMediaListApi();
                } else {
                    Toast.makeText(this, R.string.check_internet, Toast.LENGTH_SHORT).show();
                }
            } else {
                if (availableForDownload.equals(downloadedCount)) {
                    if (count == 5) {
                        count = 0;
                        if (Utils.isNetworkAvailable(this)) {
                            callGetMediaListApi();
                        } else {
                            Toast.makeText(this, R.string.check_internet, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        availableForDownload = 0;
                        downloadedCount = 0;
                        setLatestDataToDB();
                    }
                } else {
                    playFromRaw();
                }
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                if (mediaDb.checkDbIsEmpty()) {
                    playFromRaw();
                    if (Utils.isNetworkAvailable(PortraitActivity.this)) {
                        callGetMediaListApi();
                    } else {
                        Toast.makeText(PortraitActivity.this, R.string.check_internet, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (availableForDownload.equals(downloadedCount)) {
                        availableForDownload = 0;
                        downloadedCount = 0;
                        setLatestDataToDB();
                    } else {
                        playFromRaw();
                    }
                }
                return true;
            }
        });

    }

    private void setLatestDataToDB() {
        if (mediaList.size() != 0) {
            mediaDb.clearDatabase();
            Toast.makeText(this, "cleared", Toast.LENGTH_SHORT).show();
            for (int i = 0; i < mediaList.size(); i++) {
                try {
                    mediaDb.insert_data((mediaList.get(i).getSize()),
                            (mediaList.get(i).getFormat()),
                            (mediaList.get(i).getFilename()),
                            (mediaList.get(i).getStime()),
                            (mediaList.get(i).getEtime()),
                            (mediaList.get(i).getOrder()),
                            (mediaList.get(i).getEid()),
                            1,
                            (mediaList.get(i).getQrcode()),
                            (mediaList.get(i).getSdate()),
                            (mediaList.get(i).getEdate()),mediaList.get(i).getDuration());
                } catch (Exception e) {

                }
            }
        }
        getDataFromDbAndPlayMedia();
        deleteTempFolder();
    }

    private void getDataFromDbAndPlayMedia() {
        ArrayList<String> availableList = mediaDb.getList("fileName");
        for (int i = 0; i < availableList.size(); i++) {
            if (!checkIfFileExists(Environment.getExternalStorageDirectory()
                    + File.separator + Environment.DIRECTORY_DOWNLOADS +
                    File.separator + availableList.get(i))) {
                pendingDownloads.add(availableList.get(i));
            } else {
                mediaDb.updateData(availableList.get(i));
            }
        }

        fileType = mediaDb.getDownloadedFileList("format");
        fileName = mediaDb.getDownloadedFileList("fileName");
        qrUrl = mediaDb.getList("actionUrl");

        pendingDownloads = mediaDb.getPendingFileNames();


        if (!pendingDownloads.isEmpty()) {
            if (checkIfFileExists(Environment.getExternalStorageDirectory()
                    + File.separator + Environment.DIRECTORY_DOWNLOADS +
                    File.separator + pendingDownloads.get(0))) {
                mediaDb.updateData(pendingDownloads.get(0));
            } else {
                if (Utils.isNetworkAvailable(PortraitActivity.this)) {
                    callDownloadMediaFunction(pendingDownloads.get(0), true);
                } else {
                    Toast.makeText(PortraitActivity.this, R.string.check_internet, Toast.LENGTH_SHORT).show();
                }
            }
        }


        // Toast.makeText(this, String.valueOf(pendingDownloads.size()), Toast.LENGTH_SHORT).show();
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
        count++;
        handler = new Handler();
        Integer cnt = fileType.size();
        if (i < cnt) {
        } else {
            i = 0;
        }
        String type = fileType.get(i);

        String path = fileName.get(i);

        String qr = qrUrl.get(i);
        i++;
        if (type.equals("Video")) {
            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            generateQR(qr);
            if (Utils.isNetworkAvailable(PortraitActivity.this)) {
                callMediaPlayApi(fileName.get(i));
            } else {
            }
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
            generateQR(qr);
            if (Utils.isNetworkAvailable(PortraitActivity.this)) {
                callMediaPlayApi(fileName.get(i));
            } else {
            }
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
            BitMatrix bitMatrix = multiFormatWriter.encode("https://www.mediafly.in/",
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
            PowerManager pm = (PowerManager) PortraitActivity.this.getSystemService(Context.POWER_SERVICE);
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
                if (checkIfFileExists(Environment.getExternalStorageDirectory()
                        + File.separator + Environment.DIRECTORY_DOWNLOADS +
                        File.separator + fileName)) {
                    mediaDb.updateData(fileName);
                } else {
                    if (Utils.isNetworkAvailable(PortraitActivity.this)) {
                        callDownloadMediaFunction(PortraitActivity.this.fileName.get(downloadedCount), false);
                        downloadedCount++;
                    } else {
                        Toast.makeText(PortraitActivity.this, R.string.check_internet, Toast.LENGTH_SHORT).show();
                    }

                }
            }

            if (availableForDownload.equals(downloadedCount)) {
                playGraphics();
            }
            if (isComingFromPending) {
                mediaDb.updateData(fileName);
            }
            if (result != null) {
                Toast.makeText(PortraitActivity.this, "Download error: " + result, Toast.LENGTH_LONG).show();
              /*  String path = Environment.getExternalStorageDirectory() +
                        File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + fileName;
                File file = new File(path);
                */
                Log.d("Download", result);
                downloadedCount++;
            } else
                Toast.makeText(PortraitActivity.this, "File downloaded", Toast.LENGTH_SHORT).show();
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
        headers.put("deviceid", Utilities.getStringPref(this, Constants.DEVICE_ID, Constants.PREF_NAME));

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
                        Toast.makeText(PortraitActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
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
                        Utilities.setStringPreference(PortraitActivity.this, Constants.IS_LOGGED_IN,
                                "NO", Constants.PREF_NAME);

                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        if (model.getVersion().toString().equals(Utilities.getStringPref(PortraitActivity.this, Constants.APP_VERSION, Constants.PREF_NAME))) {
                            Toast.makeText(PortraitActivity.this, "Update Available", Toast.LENGTH_SHORT).show();
                        }
                    }

                } else {
                    Toast.makeText(PortraitActivity.this,
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
        headers.put("deviceid", "2");

        //  headers.put("deviceid", Utilities.getStringPref(this, Constants.DEVICE_ID, Constants.PREF_NAME));


        Call<List<MediaModel>> call = apiService.GetMedia(headers, Utilities.getIPAddress(true));

        call.enqueue(new Callback<List<MediaModel>>() {
            @Override
            public void onResponse(Call<List<MediaModel>> call, Response<List<MediaModel>> response) {
                if (response.isSuccessful()) {
                    mediaList = response.body();
                    if (!mediaList.isEmpty()) {

                        Toast.makeText(PortraitActivity.this, "refreshed", Toast.LENGTH_SHORT).show();
                        for (int i = 0; i < mediaList.size(); i++) {

                            try {
                                Boolean insertResponse = mediaDb.insert_data((mediaList.get(i).getSize()),
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
                                } else {
                                    mediaDb.update_db_data((mediaList.get(i).getSize()),
                                            (mediaList.get(i).getFormat()),
                                            (mediaList.get(i).getFilename()),
                                            (mediaList.get(i).getStime()),
                                            (mediaList.get(i).getEtime()),
                                            (mediaList.get(i).getOrder()),
                                            (mediaList.get(i).getEid()),
                                            1,
                                            (mediaList.get(i).getQrcode()),
                                            (mediaList.get(i).getSdate()),
                                            (mediaList.get(i).getEdate()),mediaList.get(i).getDuration());
                                    mediaDb.updateData(mediaList.get(i).getFilename());
                                }
                            } catch (Exception e) {

                            }
                        }
                        availableForDownload = fileName.size();

                        try {
                            if (checkIfFileExists(Environment.getExternalStorageDirectory()
                                    + File.separator + Environment.DIRECTORY_DOWNLOADS +
                                    File.separator + fileName.get(downloadedCount))) {
                                mediaDb.updateData(fileName.get(downloadedCount));
                            } else {
                                if (Utils.isNetworkAvailable(PortraitActivity.this)) {
                                    callDownloadMediaFunction(fileName.get(downloadedCount),
                                            false);
                                    downloadedCount++;
                                } else {
                                    Toast.makeText(PortraitActivity.this, R.string.check_internet, Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {

                        }
                    } else {
                        Toast.makeText(PortraitActivity.this, "Please start a campaign first.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<MediaModel>> call, Throwable t) {
                Log.e("ONFAILURE", t.toString());
                Toast.makeText(PortraitActivity.this, t + " Something went wrong!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateQR(String url) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(url,
                    BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            imageQR.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void setText() {
        if (j < newsItemList.size()) {
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

    private void deleteFromDownloads(String path) {

        File file = new File(path);
        file.delete();
        if (file.exists()) {
            try {
                file.getCanonicalFile().delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (file.exists()) {
                getApplicationContext().deleteFile(file.getName());
            }
        }
    }

    private Boolean checkIfFileExists(String path) {
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DOWNLOADS +
                File.separator + path);

        File file = new File(uri.getPath());
        return file.exists();
    }

    private void checkDelete(String path) {
        Boolean isFound = true;
        for (int i = 0; i < mediaList.size(); i++) {
            Uri uri = Uri.parse(Environment.getExternalStorageDirectory()
                    + File.separator + Environment.DIRECTORY_DOWNLOADS +
                    File.separator + mediaList.get(i).getFilename());
            File file = new File(uri.getPath());

            if (path.equals(Environment.getExternalStorageDirectory()
                    + File.separator + Environment.DIRECTORY_DOWNLOADS +
                    File.separator + mediaList.get(i).getFilename())) {

            } else {
                isFound = false;
            }
        }
        if (isFound = false) {
            deleteFromDownloads(path);
        }
    }


    private void deleteTempFolder() {
        File myDir = new File(Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DOWNLOADS);
        if (myDir.isDirectory()) {
            String[] children = myDir.list();
            // Toast.makeText(this, String.valueOf(children.length), Toast.LENGTH_SHORT).show();
            for (int i = 0; i < children.length; i++) {
                //  Toast.makeText(this, children[i], Toast.LENGTH_SHORT).show();
                checkDelete(children[i]);
            }
        }
    }

    private void callMediaPlayApi(String fileName) {
        ApiService apiService = RetroClient.getApiService();
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("apiusername", Constants.API_USER_NAME);
        headers.put("apipassword", Constants.API_PASSWORD);
        headers.put("uid", "0");
        headers.put("scode", "0");
        headers.put("deviceid", "1");

        Call<SuccessModel> call = apiService.MediaPlay(headers, fileName);

        call.enqueue(new Callback<SuccessModel>() {
            @Override
            public void onResponse(Call<SuccessModel> call, Response<SuccessModel> response) {
                if (response.isSuccessful()) {
                    Log.d("MediaPlayAPI", "success");
                }
            }

            @Override
            public void onFailure(Call<SuccessModel> call, Throwable t) {
                Log.e("ONFAILURE", t.toString());
            }
        });
    }

    private void handleTextViewVisibility() {
        if(mediaList.size()==0){
            newsText.setVisibility(View.GONE);
            headingText.setVisibility(View.GONE);
        }

    }


}
