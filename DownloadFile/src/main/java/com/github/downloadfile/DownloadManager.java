package com.github.downloadfile;

import android.content.Context;

import com.github.downloadfile.listener.FileDownloadListener;

import java.util.HashMap;
import java.util.Map;

public class DownloadManager {
    private static Context context;

    public static Context getContext() {
        if (context == null) {
            throw new IllegalStateException("please call DownloadManager.init(context)");
        }
        return context;
    }

    public static void init(Context ctx) {
        context = ctx;
    }
    static Map<String,DownloadInfo> map=new HashMap<String,DownloadInfo>();

    public static DownloadInfo download(DownloadConfig config, FileDownloadListener listener) {
        DownloadInfo downloadInfo=map.get(config.getFileDownloadUrl());
        if(downloadInfo==null){
            downloadInfo = new DownloadInfo(config, listener);
            map.put(config.getFileDownloadUrl(),downloadInfo);
        }
        downloadInfo.download();
        return downloadInfo;
    }
    public static DownloadInfo download(String url, FileDownloadListener listener) {
        DownloadConfig config=new DownloadConfig.Builder().setFileDownloadUrl(url).build();
        return download(config,listener);
    }
}
