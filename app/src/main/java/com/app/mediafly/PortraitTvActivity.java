package com.app.mediafly;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.app.mediafly.common.Constants;
import com.app.mediafly.common.MediaCountDownTimer;
import com.app.mediafly.common.NewsCountDownTimer;
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

public class PortraitTvActivity extends AppCompatActivity implements NewsCountDownTimer.ICompleteTimerListener, MediaCountDownTimer.ICompleteMediaTimerListener {

    mediaDatabase mediaDb;
    List<NewsModel> newsList = new ArrayList<>();
    ArrayList<NewsModel> newsItemList = new ArrayList<>();
    List<MediaModel> mediaList = new ArrayList<>();

    ArrayList<String> allFilesList = new ArrayList<>();
    ArrayList<String> downloadedFilesList = new ArrayList<>();
    ArrayList<String> pendingFilesList = new ArrayList<>();
    ArrayList<String> fileType = new ArrayList<>();
    ArrayList<String> qrUrl = new ArrayList<>();
    ArrayList<String> mediaDuration = new ArrayList<>();

    ImageView imageQR, imageView;
    ProgressDialog mProgressDialog;
    VideoView videoView;
    VerticalTextView headingText, newsText;

    Integer j = 0, i = 0, mediaPlay = 0;

    Boolean isDownloading = false;
    NewsCountDownTimer newsCountDownTimer;
    MediaCountDownTimer mediaCountDownTimer;
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 1000 * 60 * 60;
    int finalDur;

    @Override
    protected void onResume() {
        newsCountDownTimer = new NewsCountDownTimer(5000, 1000, this);
        newsCountDownTimer.start();
        handler.postDelayed(runnable = () -> {
            handler.postDelayed(runnable, delay);
            if (Utils.isNetworkAvailable(this)) {
                callCheckShowNewsStatusApi();
                callGetMediaListApi();
            }
        }, delay);

        if (Utils.isNetworkAvailable(this)) {
            callAppInfoApi();
        }

        super.onResume();
        if (!videoView.isPlaying()) {
            checkConditions();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mediaDb = new mediaDatabase(this);

        declareUiThings();
        hideNewsSection();

        if (Utils.isNetworkAvailable(this)) {
            playFromRaw();
            callCheckShowNewsStatusApi();
            checkConditions();
            handler = new Handler();
            handler.postDelayed(() -> {
                if (Utils.isNetworkAvailable(this)) {
                    callGetMediaListApi();
                }
            }, 1000 * 10);
        } else {
            Toast.makeText(this, R.string.check_internet, Toast.LENGTH_SHORT).show();
            hideNewsSection();
            checkConditions();
        }

        videoView.setOnCompletionListener(mediaPlayer -> checkConditions());

        videoView.setOnErrorListener((mediaPlayer, i, i1) -> {
            Log.d("playError", "error");
            checkConditions();
            return true;
        });
    }

    private void updateApp(String path, Uri uri) {
        if (Build.VERSION.SDK_INT >= 24) {
            Uri contentUri = FileProvider.getUriForFile(this, "com.app.mediafly.provider", new File(path));
            Intent install = new Intent("android.intent.action.VIEW");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            install.putExtra("android.intent.extra.NOT_UNKNOWN_SOURCE", true);
            install.setData(contentUri);
            this.startActivity(install);
        } else {
            Intent installx = new Intent("android.intent.action.VIEW");
            installx.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            installx.setDataAndType(uri, "\"application/vnd.android.package-archive\"");
            this.startActivity(installx);
        }
    }

    private void checkConditions() {
        Log.d("checkcond", "true");
        clearMediaData();
        if (mediaDb.checkDbIsEmpty()) {
            playFromRaw();
//            generateQR();
            if (Utils.isNetworkAvailable(PortraitTvActivity.this)) {
                callGetMediaListApi();
            } else {
                Toast.makeText(PortraitTvActivity.this, R.string.check_internet, Toast.LENGTH_SHORT).show();
            }
        } else {
            clearMediaData();

            allFilesList = mediaDb.getList("fileName");

            Log.d("allfilesize", String.valueOf(allFilesList.size()));
            for (int i = 0; i < allFilesList.size(); i++) {
                if (checkIfFileExists(allFilesList.get(i))) {
                    mediaDb.updateData(allFilesList.get(i));
                }
            }


            downloadedFilesList = mediaDb.getDownloadedFileList("fileName");
            fileType = mediaDb.getDownloadedFileList("format");
            pendingFilesList = mediaDb.getPendingFileNames();
            qrUrl = mediaDb.getDownloadedFileList("actionUrl");
            mediaDuration = mediaDb.getDownloadedFileList("duration");

            Log.d("downloadedFilesList", String.valueOf(downloadedFilesList.size()));
            Log.d("fileType", String.valueOf(fileType.size()));
            Log.d("pendingFilesList", String.valueOf(pendingFilesList.size()));

            if (allFilesList.size() == downloadedFilesList.size()) {
                for (int i = 0; i < downloadedFilesList.size(); i++) {
                    if (!checkIfFileExists(downloadedFilesList.get(i))) {
                        pendingFilesList.add(downloadedFilesList.get(i));
                        downloadedFilesList.remove(downloadedFilesList.get(i));
                        fileType.remove(fileType.get(i));
                        qrUrl.remove(qrUrl.get(i));
                        mediaDuration.remove(mediaDuration.get(i));
                    }
                }

                if (!pendingFilesList.isEmpty()) {
                    if (Utils.isNetworkAvailable(this)) {
                        for (int i = 0; i < pendingFilesList.size(); i++) {
                            if (!checkIfFileExists(pendingFilesList.get(i))) {
                                if (!isDownloading) {
                                    callDownloadMediaFunction(pendingFilesList.get(0), false);
                                }
                            } else {
                                mediaDb.updateData(pendingFilesList.get(i));
                            }
                        }
                    }
                }
            } else {
                if (!downloadedFilesList.isEmpty()) {
                    for (int i = 0; i < downloadedFilesList.size(); i++) {
                        if (!checkIfFileExists(downloadedFilesList.get(i))) {
                            pendingFilesList.add(downloadedFilesList.get(i));
                            downloadedFilesList.remove(downloadedFilesList.get(i));
                            fileType.remove(fileType.get(i));
                            qrUrl.remove(qrUrl.get(i));
                            mediaDuration.remove(mediaDuration.get(i));
                        }
                    }

                    if (!pendingFilesList.isEmpty()) {
                        if (checkIfFileExists(pendingFilesList.get(0))) {
                            mediaDb.updateData(pendingFilesList.get(0));
                            checkConditions();
                        } else {
                            if (Utils.isNetworkAvailable(this)) {
                                if (!isDownloading) {
                                    callDownloadMediaFunction(pendingFilesList.get(0), false);
                                }
                            }
                        }
                    }
                } else {
                    if (!isDownloading) {
                        callDownloadMediaFunction(pendingFilesList.get(0), false);
                    }
                }
            }

          /*  Log.d("pendingsize", String.valueOf(pendingFilesList.size()));
            Log.d("allsize", String.valueOf(allFilesList.size()));
            Log.d("downloadsize", String.valueOf(downloadedFilesList.size()));
*/
            if (pendingFilesList.isEmpty()) {
                playGraphics();
            } else playFromRaw();
        }
    }

    private void playGraphics() {
        handler = new Handler();
        Integer cnt = fileType.size();
        if (i < cnt) {
        } else {
            i = 0;
        }

        String type = fileType.get(i);
        String path = downloadedFilesList.get(i);
        String duration = mediaDuration.get(i);

        String qr = qrUrl.get(i);
        if (Utils.isNetworkAvailable(PortraitTvActivity.this)) {
            if (!downloadedFilesList.isEmpty())
                callMediaPlayApi(downloadedFilesList.get(i));
        }
        if (type.equals("Video")) {
            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            generateQR(qr);
            try {
                videoView.setVideoURI(Uri.parse(Environment.getExternalStorageDirectory()
                        + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + path));
                videoView.start();
            } catch (Exception e) {
                Log.d("error", e.toString());
                //  Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            videoView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            generateQR(qr);
            try {
                Uri uri = Uri.parse(Environment.getExternalStorageDirectory()
                        + File.separator + Environment.DIRECTORY_DOWNLOADS +
                        File.separator + path);
                imageView.setImageURI(uri);
            } catch (Exception e) {
                Log.d("error", e.toString());
                //  Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            int dur = 0;
            if (duration.equals("0")) {
                dur = 10 * 1000;
            } else {
                dur = 1000 * Integer.parseInt(duration);
            }
             finalDur = dur;

            mediaCountDownTimer = new MediaCountDownTimer(finalDur, 1000, this);
            mediaCountDownTimer.start();
            // handler.postDelayed(this::checkConditions, finalDur);
        }

        mediaPlay++;
        i++;

    }

    private void clearMediaData() {
        downloadedFilesList.clear();
        pendingFilesList.clear();
        allFilesList.clear();
        fileType.clear();
        qrUrl.clear();
    }

    private void playFromRaw() {
        String path = "android.resource://" + getPackageName() + "/" + "raw/" + "max_video.mp4";
        videoView.setVideoURI(Uri.parse(path));
        videoView.start();
        generateQR(Utilities.getStringPref(this, Constants.QR, Constants.PREF_NAME));
    }

    //check if file exists in storage
    private Boolean checkIfFileExists(String path) {
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DOWNLOADS +
                File.separator + path);

        File file = new File(uri.getPath());
        return file.exists();
    }

    //delete from downloads
    private void deleteFromDownloads(String path) {
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DOWNLOADS +
                File.separator + path);

        File file = new File(uri.getPath());
        file.delete();
        if (file.exists()) {
            try {
                file.getCanonicalFile().delete();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("error", e.toString());
            }
            if (file.exists()) {
                getApplicationContext().deleteFile(file.getName());
            }
        }

        Log.d("fileDelete", String.valueOf(checkIfFileExists(file.getPath())));
    }

    private void deleteRemovedFiles() {
        String path = Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DOWNLOADS;
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        Log.d("Files", "Size: " + files.length);
        for (int i = 0; i < files.length; i++) {
            Log.d("Files", "FileName:" + path + "/" + files[i].getName());
            boolean exists = false;
            for (int j = 0; j < allFilesList.size(); j++) {
                Log.d("Files", "FileName:" + path + "/" + allFilesList.get(j));
                if (files[i].getName().equals(allFilesList.get(j))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                deleteFromDownloads(files[i].getName());
            }
        }

    }

    //Generate qr hard codely
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
            Log.d("error", e.toString());
        }
    }

    //Get news From API
    private void callGetNewsListApi() {
        ApiService apiService = RetroClient.getApiService();

        HashMap<String, String> headers = new HashMap<>();
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
                            newsItemList.add(newsList.get(i));
                        }
                        setText();
                    } else {
                        //    Toast.makeText(PortraitTvActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<NewsModel>> call, Throwable t) {
                Log.e("ONFAILURE", t.toString());
            }
        });
    }

    //Set text and reset to zero position
    private void setText() {
        if (j < newsItemList.size()) {
            headingText.setText(newsItemList.get(j).getTitle());
            newsText.setText(newsItemList.get(j).getDescription());
            j++;
        } else {
            j = 0;
        }
    }

    //declare ui things
    private void declareUiThings() {
        videoView = findViewById(R.id.videoView);
        headingText = findViewById(R.id.headingText);
        newsText = findViewById(R.id.newsText);
        imageQR = findViewById(R.id.imageQR);
        imageView = findViewById(R.id.imageView);
    }

    //generate qr from url
    private void generateQR(String url) {
        if (url.isEmpty()) {
            imageQR.setVisibility(View.GONE);
        } else {
            imageQR.setVisibility(View.VISIBLE);
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            try {
                BitMatrix bitMatrix = multiFormatWriter.encode(url,
                        BarcodeFormat.QR_CODE, 200, 200);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                imageQR.setImageBitmap(bitmap);
            } catch (WriterException e) {
                e.printStackTrace();
                Log.d("error", e.toString());
            }
        }

    }

    //download file
    private void callDownloadMediaFunction(String fileName, Boolean isComingForApk) {
        mProgressDialog = new ProgressDialog(this);
        isDownloading = true;
        if (isComingForApk) {
            mProgressDialog.setMessage("Downloading Latest APK");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
        } else {
            mProgressDialog.setMessage("Downloading Resources");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
        }

        if (isComingForApk) {
            final PortraitTvActivity.DownloadTask downloadTask = new PortraitTvActivity.DownloadTask("Relaxo.apk", true);
            try {
                downloadTask.execute(fileName);
            } catch (Exception e) {
                Log.d("error", e.toString());
                downloadTask.cancel(true);
            }

        } else {
            final PortraitTvActivity.DownloadTask downloadTask = new PortraitTvActivity.DownloadTask(fileName, false);
            try {
                downloadTask.execute(Constants.BASE_URL + fileName);
            } catch (Exception e) {
                Log.d("error", e.toString());
                downloadTask.cancel(true);
            }
        }
        // execute this when the downloader must be fired


        mProgressDialog.setOnCancelListener(dialog -> {
            //downloadTask.cancel(true); //cancel the task
        });
    }

    @Override
    public void onCompleteTimer(@NonNull String action) {
        setText();
        Log.d("resumenews", "yes");
        newsCountDownTimer.start();
    }

    @Override
    public void onCompleteMediaTimer(@NonNull String action) {
        // callAppInfoApi();
        checkConditions();
        Log.d("resumecontent", "yes");

    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private final String fileName;
        private final Boolean isComingForApk;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(String fileName, Boolean isComingForApk) {
            this.fileName = fileName;
            this.isComingForApk = isComingForApk;
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
                String path = "";
                if (isComingForApk) {
                    path = Environment.getExternalStorageDirectory() +
                            File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + "Relaxo.apk";
                } else {
                    path = Environment.getExternalStorageDirectory() +
                            File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + fileName;
                }

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
                Log.d("error", e.toString());
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                    Log.d("error", ignored.toString());
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
            PowerManager pm = (PowerManager) PortraitTvActivity.this.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();

        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
            // if we get here, length is known, now set indeterminate to false
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            isDownloading = false;
            if (result != null) {
                //    Toast.makeText(PortraitTvActivity.this, "Download error: " + result, Toast.LENGTH_LONG).show();
                Log.d("Download Error", result);
                checkConditions();
            } else {
                //    Toast.makeText(PortraitTvActivity.this, "File downloaded", Toast.LENGTH_SHORT).show();
                if (isComingForApk) {
                    String destination = Environment.getExternalStorageDirectory() +
                            File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + "Relaxo.apk";
                    Uri uri = Uri.parse("file://" + destination);

                    updateApp(destination, uri);
                } else {
                    mediaDb.updateData(fileName);
                    checkConditions();
                  /*  if (!pendingFilesList.isEmpty()) {
                        callDownloadMediaFunction(pendingFilesList.get(0), false);
                    }*/
                }

            }
        }
    }

    //GetContentFilesFromAPI
    private void callGetMediaListApi() {
        ApiService apiService = RetroClient.getApiService();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("apiusername", Constants.API_USER_NAME);
        headers.put("apipassword", Constants.API_PASSWORD);
        headers.put("uid", "0");
        headers.put("scode", "0");
        headers.put("deviceid", (Utilities.getStringPref(this, Constants.DEVICE_ID, Constants.PREF_NAME)));

        Call<List<MediaModel>> call = apiService.GetMedia(headers, Utilities.getIPAddress(true));

        call.enqueue(new Callback<List<MediaModel>>() {
            @Override
            public void onResponse(Call<List<MediaModel>> call, Response<List<MediaModel>> response) {
                if (response.isSuccessful()) {
                    mediaList = response.body();
                    if (!mediaList.isEmpty()) {

                        Log.d("hitMediaApi", "API HIT");
                      /*  if (!allFilesList.isEmpty()) {
                            playFromRaw();
                            for (int i = 0; i < allFilesList.size(); i++) {
                                if (checkIfFileExists(allFilesList.get(i))) {
                                    deleteFromDownloads(allFilesList.get(i));
                                }
                            }
                        }*/
                        clearMediaData();
                        mediaDb.clearDatabase();

                        Log.d("dbIsEmpty", String.valueOf(mediaDb.checkDbIsEmpty()));

                        for (int i = 0; i < mediaList.size(); i++) {
                            try {
                                boolean insertResponse = mediaDb.insert_data((mediaList.get(i).getSize()),
                                        (mediaList.get(i).getFormat()),
                                        (mediaList.get(i).getFilename()),
                                        (mediaList.get(i).getStime()),
                                        (mediaList.get(i).getEtime()),
                                        (mediaList.get(i).getOrder()),
                                        (mediaList.get(i).getEid()),
                                        0,
                                        (mediaList.get(i).getQrcode()),
                                        (mediaList.get(i).getSdate()),
                                        (mediaList.get(i).getEdate()),
                                        mediaList.get(i).getDuration());
                            } catch (Exception e) {
                                Log.d("error", e.toString());
                                e.printStackTrace();
                            }
                        }
                        allFilesList = mediaDb.getList("fileName");

                        if (!isDownloading) {
                            for (int j = 0; j < allFilesList.size(); j++) {
                                if (checkIfFileExists(allFilesList.get(j))) {
                                    mediaDb.updateData(allFilesList.get(j));
                                }
                            }
                        }

                        deleteRemovedFiles();
                        checkConditions();
                    }
                } else {
                    Toast.makeText(PortraitTvActivity.this, "Please start a campaign first.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<MediaModel>> call, Throwable t) {
                Log.e("ONFAILURE", t.toString());
                Toast.makeText(PortraitTvActivity.this, t + " Something went wrong!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void callMediaPlayApi(String fileName) {
        ApiService apiService = RetroClient.getApiService();
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("apiusername", Constants.API_USER_NAME);
        headers.put("apipassword", Constants.API_PASSWORD);
        headers.put("uid", "0");
        headers.put("scode", "0");
        headers.put("deviceid", Utilities.getStringPref(this, Constants.DEVICE_ID, Constants.PREF_NAME));

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

    private void callCheckShowNewsStatusApi() {
        ApiService apiService = RetroClient.getApiService();
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("apiusername", Constants.API_USER_NAME);
        headers.put("apipassword", Constants.API_PASSWORD);
        headers.put("uid", "0");
        headers.put("scode", "0");
        headers.put("deviceid", Utilities.getStringPref(this, Constants.DEVICE_ID, Constants.PREF_NAME));

        Call<SuccessModel> call = apiService.CheckShowNewsStatus(headers);

        call.enqueue(new Callback<SuccessModel>() {
            @Override
            public void onResponse(Call<SuccessModel> call, Response<SuccessModel> response) {
                if (response.isSuccessful()) {

                    SuccessModel model = response.body();

                    if (model.getStatus().equalsIgnoreCase("true")) {
                        showNewsSection();
                        callGetNewsListApi();
                    } else {
                        hideNewsSection();
                    }
                    Log.d("MediaPlayAPI", "success");
                }
            }

            @Override
            public void onFailure(Call<SuccessModel> call, Throwable t) {
                Log.e("ONFAILURE", t.toString());
            }
        });
    }

    private void hideNewsSection() {
        headingText.setVisibility(View.GONE);
        newsText.setVisibility(View.GONE);
    }

    private void showNewsSection() {
        headingText.setVisibility(View.VISIBLE);
        newsText.setVisibility(View.VISIBLE);
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
/*
                    Log.d("validversion", model.getIsValidDevice());
                    Log.d("version", model.getVersion().toString());*/

                    if (model.getIsValidDevice().equals("false")) {
                        Utilities.setStringPreference(PortraitTvActivity.this, Constants.IS_LOGGED_IN,
                                "NO", Constants.PREF_NAME);


                        if (newsCountDownTimer != null) {
                            newsCountDownTimer.cancel();
                        }
                        if (mediaCountDownTimer != null) {
                            mediaCountDownTimer.cancel();
                        }
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                        finish();

                    } else {
                        if (model.getVersion().toString()!=String.valueOf(BuildConfig.VERSION_CODE)) {
                            Toast.makeText(PortraitTvActivity.this, "Update Available", Toast.LENGTH_SHORT).show();

                            Log.d("modelVersion",model.getVersion().toString());
                            Log.d("buildVersion",String.valueOf(BuildConfig.VERSION_CODE));

                            String destination = Environment.getExternalStorageDirectory() +
                                    File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + "Relaxo.apk";

                            String url = model.getLink();
                            File file = new File(destination);
                            if (file.exists()) {
                                deleteFromDownloads("Relaxo.apk");
                            }

                            if (!isDownloading) {
                                callDownloadMediaFunction(url, true);
                            }
                        }
                    }

                } else {
                    Toast.makeText(PortraitTvActivity.this,
                            "Something went wrong!",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AppInfoModel> call, Throwable t) {
                Log.e("ONFAILURE", t.toString());
                //   Toast.makeText(PortraitTvActivity.this, t.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }


}
