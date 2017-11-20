package com.stkay.gyaonandroid;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

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
    private final String TAG = "Gyazo";

    private Context context;

    private ContentResolver contentResolver;

    GyazoUploader(Context context) {
        this.context = context;
        this.contentResolver = this.context.getContentResolver();
    }

    void uploadImage(final Uri uri) {
        OkHttpClient client = new OkHttpClient();
        String type = this.contentResolver.getType(uri);
        File file = getFileFromContentUri(uri);

        if(file != null) {

            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("id", "bfe375d0b3ec5c50f339ebdda59b9ff8a96298cb0fb59782d20b24fcf587cdf0")
                    .addFormDataPart("access_token", "00252e72bef882b4849ba9246e751f9515b461be068a9cfe02037f710c0f8192")
                    .addFormDataPart("imagedata", "gyazo", RequestBody.create(MediaType.parse(type), file))
                    .build();

            Request request = new Request.Builder()
                    .url("https://upload.gyazo.com/api/upload")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Toast.makeText(context, "Failed to upload to Gyazo", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Failed to upload to Gyazo");
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Toast.makeText(context, "Succeeded to upload to Gyazo", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "gyazo url: " + response.body().string());
                }
            });
        }
    }

    @Nullable
    private File getFileFromContentUri(Uri uri) {
        Cursor cursor = this.contentResolver.query(uri, null, null, null, null);
        if (cursor != null) {
            try {

                if (cursor.moveToFirst()) {
                    if (cursor.getLong(1) > 0)
                        return new File(this.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + cursor.getString(0));
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }
}
