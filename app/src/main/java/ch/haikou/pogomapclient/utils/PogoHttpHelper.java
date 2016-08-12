package ch.haikou.pogomapclient.utils;

import android.util.Log;


import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Zoruk on 08.08.2016.
 */

public class PogoHttpHelper {

    private static final String TAG = PogoHttpHelper.class.getName();

    public static String request(HttpUrl url, String username, String password) {


        OkHttpClient okhttp = new OkHttpClient();
        Request.Builder okrequest = new Request.Builder();
        if (username != null && password != null) {
            okrequest.addHeader("Authorization",
                    Credentials.basic(username, password));
        }
        Response response = null;
        try {
            response = okhttp.newCall(okrequest.url(url).build()).execute();
            if (!response.isSuccessful()) {
                Log.d(TAG, "Request failed:" + response.message());
                return null;
            } else {
                Log.d(TAG, "Success");
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }
}
