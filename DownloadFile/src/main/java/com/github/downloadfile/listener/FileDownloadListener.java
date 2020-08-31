package com.github.downloadfile.listener;

import java.io.File;

public interface FileDownloadListener {
    void onConnect(long totalSize);
    void onSpeed(float speedBySecond);
    void onProgress(long progress,long totalSize);
    void onSuccess(File file);
    void onPause();
    void onDelete();
    void onError();
}
