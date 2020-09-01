package com.github.downloadfile;

import android.content.Context;

import com.github.downloadfile.listener.FileDownloadListener;

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

    public static DownloadInfo download(DownloadConfig config, FileDownloadListener listener) {
        DownloadInfo downloadInfo = new DownloadInfo(config, listener);
        downloadInfo.download();
        return downloadInfo;
    }
    public static DownloadInfo download(String url, FileDownloadListener listener) {
        DownloadConfig config=new DownloadConfig.Builder().setFileDownloadUrl(url).build();
        DownloadInfo downloadInfo = new DownloadInfo(config, listener);
        downloadInfo.download();
        return downloadInfo;
    }
}
