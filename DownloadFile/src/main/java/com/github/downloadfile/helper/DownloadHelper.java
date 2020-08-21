package com.github.downloadfile.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.github.downloadfile.DownloadManager;
import com.github.downloadfile.bean.DownloadRecord;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadHelper {
    /**********************************************************/
    private static DownloadHelper singleObj;
    private SharedPreferences sp;

    private DownloadHelper() {
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }

    public static DownloadHelper get() {
        if (singleObj == null) {
            synchronized (DownloadHelper.class) {
                if (singleObj == null) {
                    singleObj = new DownloadHelper();
                }
            }
        }
        return singleObj;
    }

    /**********************************************************/

    private Handler handler;
    private ExecutorService executorService;

    public Handler getHandler() {
        return handler;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void flush(Flushable flushable) {
        if (flushable == null) {
            return;
        }
        try {
            flushable.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile(File file) {
        if (file == null) {
            return;
        }
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

    private final String sp_file_name = "multi_download_sp";

    public DownloadRecord getRecord(String uniqueId) {
        SharedPreferences sp = DownloadManager.getContext().getSharedPreferences(sp_file_name, Context.MODE_PRIVATE);
        String downloadRecord = sp.getString(uniqueId, null);
        return DownloadRecord.fromJson(downloadRecord);
    }

    public void saveRecord(DownloadRecord downloadRecord) {
        if(downloadRecord==null){
            return;
        }
        String json = downloadRecord.toJson();
        String key = downloadRecord.getUniqueId();
        if(sp==null){
            sp = DownloadManager.getContext().getSharedPreferences(sp_file_name, Context.MODE_PRIVATE);
        }
        sp.edit().putString(key,json).apply();
    }
    public void clearRecord(String uniqueId) {
        if(sp==null){
            sp = DownloadManager.getContext().getSharedPreferences(sp_file_name, Context.MODE_PRIVATE);
        }
        sp.edit().remove(uniqueId).commit();
    }
}
