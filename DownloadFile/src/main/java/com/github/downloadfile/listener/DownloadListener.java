package com.github.downloadfile.listener;

import java.io.File;

public interface DownloadListener {
    void onStartDownload(long fileSizeKB);
    void onProgress(long downloadSizeKB);
    void onSuccess(File file);
    void onError(int HTTPStatusCode);
}
