package com.github.downloadfile;

import android.app.Application;
import android.content.Context;

import com.github.downloadfile.listener.FileDownloadListener;


public class FileDownloadManager {
    public static boolean debug=false;
    private static Context context;

    public static Context getContext() {
        if (context == null) {
            throw new IllegalStateException("please call FileDownloadManager.init(context)");
        }
        return context;
    }

    public static void init(Context ctx) {
        if(context!=null){
            return;
        }
        context = ctx.getApplicationContext();
    }
    public static void init(Context ctx,boolean isDebug) {
        init(ctx);
        setDebug(isDebug);
    }

    public static void setDebug(boolean debug) {
        FileDownloadManager.debug=debug;
    }

    public static DownloadInfo download(DownloadConfig config, FileDownloadListener listener) {
        DownloadInfo downloadInfo= new DownloadInfo(config, listener);
        downloadInfo.download();
        return downloadInfo;
    }
    public static DownloadInfo download(String url, FileDownloadListener listener) {
        DownloadConfig config=new DownloadConfig.Builder().setFileDownloadUrl(url).build();
        return download(config,listener);
    }
    public static void pauseDownload(DownloadInfo downloadInfo){
        if(downloadInfo==null){
            return;
        }
        downloadInfo.pauseDownload();
    }
    public static void deleteDownload(DownloadInfo downloadInfo){
        deleteDownload(downloadInfo,false);
    }
    public static void deleteDownload(DownloadInfo downloadInfo,boolean deleteFile){
        if(downloadInfo==null){
            return;
        }
        downloadInfo.deleteDownload(deleteFile);
    }
}
