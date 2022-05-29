package com.app.mediafly;

import android.app.ProgressDialog;
import android.content.Context;
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

public class PortraitTvActivity extends AppCompatActivity {

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

    Integer j = 0, i = 0;

    Handler handler = new Handler();
    Runnable runnable;
    int delay = 1000 * 60*60;

    @Override
    protected void onResume() {
        handler.postDelayed(runnable = () -> {
            handler.postDelayed(runnable, delay);
            setText();
            if (Utils.isNetworkAvailable(this)) {
                callGetMediaListApi();
            } else {
                Toast.makeText(this, R.string.check_internet, Toast.LENGTH_SHORT).show();
            }
        }, delay);
        super.onResume();
        checkConditions();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mediaDb = new mediaDatabase(this);
        declareUiThings();


        if (Utils.isNetworkAvailable(this)) {
            callGetNewsListApi();
            callGetMediaListApi();
        } else {
            Toast.makeText(this, R.string.check_internet, Toast.LENGTH_SHORT).show();
            headingText.setVisibility(View.GONE);
            newsText.setVisibility(View.GONE);
        }


        videoView.setOnCompletionListener(mediaPlayer -> {
            checkConditions();
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.d("playError","error");
                checkConditions();
                return true;
            }
        });
    }


    private void checkConditions() {
        clearMediaData();
        if (mediaDb.checkDbIsEmpty()) {
            playFromRaw();
            generateQR();
            if (Utils.isNetworkAvailable(PortraitTvActivity.this)) {
                callGetMediaListApi();
            } else {
                Toast.makeText(PortraitTvActivity.this, R.string.check_internet, Toast.LENGTH_SHORT).show();
            }
        } else {
            clearMediaData();

            allFilesList = mediaDb.getList("fileName");
            downloadedFilesList = mediaDb.getDownloadedFileList("fileName");
            fileType = mediaDb.getDownloadedFileList("format");
            pendingFilesList = mediaDb.getPendingFileNames();
            qrUrl = mediaDb.getDownloadedFileList("actionUrl");
            mediaDuration = mediaDb.getDownloadedFileList("duration");


             if (allFilesList.size() == downloadedFilesList.size()) {
                 for(int i = 0;i<downloadedFilesList.size();i++){
                     if(!checkIfFileExists(downloadedFilesList.get(i))){
                         pendingFilesList.add(downloadedFilesList.get(i));
                     }
                 }
                playGraphics();
            } else {
                if (!downloadedFilesList.isEmpty()) {
                    playGraphics();
                } else {
                    playFromRaw();
                }
                if (!pendingFilesList.isEmpty()) {
                    if (checkIfFileExists(pendingFilesList.get(0))) {
                        mediaDb.updateData(pendingFilesList.get(0));
                        checkConditions();
                    } else callDownloadMediaFunction(pendingFilesList.get(0));
                }
            }
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
            callMediaPlayApi(downloadedFilesList.get(i));
        } else {
        }
        i++;

        if (type.equals("Video")) {

            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            generateQR(qr);
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
            try {
                Uri uri = Uri.parse(Environment.getExternalStorageDirectory()
                        + File.separator + Environment.DIRECTORY_DOWNLOADS +
                        File.separator + path);
                imageView.setImageURI(uri);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            handler.postDelayed(() -> playGraphics(), Integer.parseInt(duration));

        }
    }


    private void clearMediaData() {
        downloadedFilesList.clear();
        pendingFilesList.clear();
        allFilesList.clear();
        fileType.clear();
        qrUrl.clear();
    }

    private void playFromRaw() {
        String path = "android.resource://" + getPackageName() + "/" + R.raw.mediafly;
        videoView.setVideoURI(Uri.parse(path));
        videoView.start();
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
            }
            if (file.exists()) {
                getApplicationContext().deleteFile(file.getName());
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
        }
    }

    //Get news From API
    private void callGetNewsListApi() {
        ApiService apiService = RetroClient.getApiService();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("apiusername", Constants.API_USER_NAME);
        headers.put("apipassword", Constants.API_PASSWORD);
        headers.put("uid", "0");
        headers.put("scode", "0");
        headers.put("deviceid", "8");
        //   headers.put("deviceid", Utilities.getStringPref(this, Constants.DEVICE_ID, Constants.PREF_NAME));

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
                        Toast.makeText(PortraitTvActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
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
            }
        }

    }


    //download file
    private void callDownloadMediaFunction(String fileName) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Downloading Resources");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        // execute this when the downloader must be fired
        final PortraitTvActivity.DownloadTask downloadTask = new PortraitTvActivity.DownloadTask(this, fileName);
        downloadTask.execute(Constants.BASE_URL + fileName);

        mProgressDialog.setOnCancelListener(dialog -> {
            downloadTask.cancel(true); //cancel the task
        });
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private final Context context;
        private String fileName = "";
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context, String fileName) {
            this.context = context;
            this.fileName = fileName;
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
            PowerManager pm = (PowerManager) PortraitTvActivity.this.getSystemService(Context.POWER_SERVICE);
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

            if (result != null) {
                Toast.makeText(PortraitTvActivity.this, "Download error: " + result, Toast.LENGTH_LONG).show();
                Log.d("Download Error", result);
            } else {
                Toast.makeText(PortraitTvActivity.this, "File downloaded", Toast.LENGTH_SHORT).show();
                mediaDb.updateData(fileName);
                checkConditions();
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
        headers.put("deviceid", "8");

        //  headers.put("deviceid", Utilities.getStringPref(this, Constants.DEVICE_ID, Constants.PREF_NAME));


        Call<List<MediaModel>> call = apiService.GetMedia(headers, Utilities.getIPAddress(true));

        call.enqueue(new Callback<List<MediaModel>>() {
            @Override
            public void onResponse(Call<List<MediaModel>> call, Response<List<MediaModel>> response) {
                if (response.isSuccessful()) {
                    mediaList = response.body();
                    if (!mediaList.isEmpty()) {

                        Log.d("hitMediaApi","API HIT");
                        if(!allFilesList.isEmpty()){
                            playFromRaw();
                            for(int i =0;i<allFilesList.size();i++){
                                if(checkIfFileExists(allFilesList.get(i))){
                                    deleteFromDownloads(allFilesList.get(i));
                                }
                            }
                        }
                        clearMediaData();
                        mediaDb.clearDatabase();

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
                                e.printStackTrace();
                            }
                        }
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
        headers.put("deviceid", "8");

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

}
