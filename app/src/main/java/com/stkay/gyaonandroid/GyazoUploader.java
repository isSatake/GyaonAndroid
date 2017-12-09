package com.stkay.gyaonandroid;

import android.util.Log;
import android.webkit.MimeTypeMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by stk on 2017/11/20.
 */

class GyazoUploader {
    static void uploadImage(final File file, final String gyaonKey, final MainActivity.GyazoListener gyazoListener) {
        final String TAG = "Gyazo";
        final OkHttpClient gyazoUpClient = new OkHttpClient();
        final String type = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());

        RequestBody gyazoBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id", "bfe375d0b3ec5c50f339ebdda59b9ff8a96298cb0fb59782d20b24fcf587cdf0")
                .addFormDataPart("access_token", "00252e72bef882b4849ba9246e751f9515b461be068a9cfe02037f710c0f8192")
                .addFormDataPart("imagedata", "gyazo", RequestBody.create(MediaType.parse(type), file))
                .build();

        Request request = new Request.Builder()
                .url("https://upload.gyazo.com/api/upload")
                .post(gyazoBody)
                .build();

        gyazoUpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "Failed to upload to Gyazo");
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject res = null;
                String url = null;
                try {
                    res = new JSONObject(response.body().string());
                    url = res.get("url").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (url != null) {
                    Log.d(TAG, "Succeeded to upload image " + url);
                    gyazoListener.onUpload();
                    final OkHttpClient updateLinkClient = new OkHttpClient();
                    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    final RequestBody linkBody = RequestBody.create(JSON, "{\"imgurl\": \"" + url + "\"}");

                    Request request = new Request.Builder()
                            .url("https://gyaon.herokuapp.com/image/" + gyaonKey)
                            .post(linkBody)
                            .build();


                    updateLinkClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.d(TAG, "Failed to link image");
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            Log.d(TAG, "Succeeded to link image");
                            gyazoListener.onLink();
                        }
                    });
                }
            }
        });
    }
}
