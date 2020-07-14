package com.github.downloadfile.listener;

import java.io.File;

public interface DownloadListener {
    void onConnect(long fileSizeKB);
    void onProgress(long downloadSizeKB);
    void onSuccess(File file);
    void onPause();
    void onCancel();
    void onError();
}
