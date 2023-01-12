package com.github.downloadfile;

import android.util.Log;

class LG {
    public static void i(String content) {
        if (!FileDownloadManager.debug) {
            return;
        }
        Log.i("Download", content);
    }
}
