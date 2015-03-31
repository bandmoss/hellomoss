package com.bandmoss.hellomoss.util;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by rok on 2015. 3. 29..
 */
public class HttpGetTask extends AsyncTask<Void, Void, String> {

    private String requestUrl;
    private Callback<String> callback;

    public HttpGetTask(String requestUrl, Callback<String> callback) {
        this.requestUrl = requestUrl;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void[] objects) {
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(false);

            if (connection.getResponseCode() == 200) {

                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, "utf-8"));
                String line;
                StringBuffer response = new StringBuffer();

                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }

                connection.disconnect();

                return response.toString();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if(callback != null) {
            callback.callback(result);
        }
    }
}
