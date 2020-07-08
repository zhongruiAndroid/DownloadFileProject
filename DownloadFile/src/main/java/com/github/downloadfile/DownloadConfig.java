package com.github.downloadfile;

import android.app.AlertDialog;

import java.io.Serializable;

public class DownloadConfig implements Serializable {
    private String filePath;

    private String fileName;
    private String tempFileNameSuffix;

    protected DownloadConfig() {
    }

    public static class Builder{
        private DownloadConfig config;

        public Builder() {
            config=new DownloadConfig();
        }
        AlertDialog bb=new AlertDialog.Builder(null).setTitle("").create();

        public DownloadConfig build(){
            DownloadConfig downloadConfig=new DownloadConfig();

            return downloadConfig;
        }
    }
}
