package com.github.downloadfile.listener;

import java.io.File;

public interface DownloadListener {
    void onConnect(long totalSize);
    void onProgress(long progress);
    void onSuccess(File file);
    void onPause();
    void onCancel();
    void onError();
}
