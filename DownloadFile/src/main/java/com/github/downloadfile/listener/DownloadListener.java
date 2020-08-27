package com.github.downloadfile.listener;

import java.io.File;

public interface DownloadListener {
    void onConnect(long totalSize);
    void onProgress(long progress,long totalSize);
    void onSuccess(File file);
    void onPause();
    void onDelete();
    void onError();
}
