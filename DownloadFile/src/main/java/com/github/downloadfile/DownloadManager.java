package com.github.downloadfile;

import android.text.TextUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadManager {
    public static void test(String imageUrl){
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range","bytes="+0+"-");
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                httpURLConnection.getContentLength();
                String ranges = httpURLConnection.getRequestProperty("Accept-Ranges");
                if(TextUtils.isEmpty(ranges)||!"bytes".equalsIgnoreCase(ranges)){

                }
                InputStream inputStream = httpURLConnection.getInputStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {

        }

    }
}
