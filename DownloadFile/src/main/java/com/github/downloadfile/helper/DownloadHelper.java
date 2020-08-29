package com.github.downloadfile.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import com.github.downloadfile.DownloadManager;
import com.github.downloadfile.bean.DownloadRecord;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.List;
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

    public DownloadRecord getRecord(String downloadFileUrl) {
        SharedPreferences sp = DownloadManager.getContext().getSharedPreferences(sp_file_name, Context.MODE_PRIVATE);
        String downloadRecord = sp.getString(downloadFileUrl.hashCode() + "", null);
        return DownloadRecord.fromJson(downloadRecord);
    }

    public void saveRecord(DownloadRecord downloadRecord) {
        if (downloadRecord == null) {
            return;
        }
        String json = downloadRecord.toJson();
        String key = downloadRecord.getUniqueId();
        if (sp == null) {
            sp = DownloadManager.getContext().getSharedPreferences(sp_file_name, Context.MODE_PRIVATE);
        }
        sp.edit().putString(key, json).apply();
    }

    public void clearRecord(String downloadFileUrl) {
        if (sp == null) {
            sp = DownloadManager.getContext().getSharedPreferences(sp_file_name, Context.MODE_PRIVATE);
        }
        sp.edit().remove(downloadFileUrl.hashCode() + "").commit();
    }

    public static boolean hasFreeSpace(Context context, long downloadSize) {
        if (context == null || downloadSize <= 0) {
            return true;
        }
        long space;
        File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir == null) {
            space = -1;
        } else {
            space = externalCacheDir.getFreeSpace();
        }
        File filesDir = context.getFilesDir();
        if (space != -1) {
            space = Math.min(space, filesDir.getFreeSpace());
        } else {
            space = filesDir.getFreeSpace();
        }
        return space > downloadSize;
    }

    public Pair<Long, Long> getProgressByUrl(String fileDownloadUrl) {
        DownloadRecord record = getRecord(fileDownloadUrl);
        if (record == null || record.getFileSize() <= 0) {
            return new Pair(new Long(0), new Long(0));
        }
        List<DownloadRecord.FileRecord> fileRecordList = record.getFileRecordList();
        if (fileRecordList == null || fileRecordList.isEmpty()) {
            return new Pair(new Long(0), new Long(0));
        }
        long localCacheSize = 0;
        for (DownloadRecord.FileRecord fileRecord : fileRecordList) {
            localCacheSize += fileRecord.getDownloadLength();
        }
        return new Pair(new Long(localCacheSize), new Long(record.getFileSize()));
    }
}
