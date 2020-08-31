package com.github.downloadfile;

import android.content.Context;
import android.text.TextUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadManager {
    private static Context context;

    public static Context getContext() {
        if(context==null){
            throw new IllegalStateException("please call DownloadManager.init(context)");
        }
        return context;
    }
    public static void init(Context ctx){
        context=ctx;
    }

    public static void download(String url){

    }
    public static void download(String url,DownloadInfo  downloadInfo){

    }
}
