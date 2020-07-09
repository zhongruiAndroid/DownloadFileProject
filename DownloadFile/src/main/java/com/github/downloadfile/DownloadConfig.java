package com.github.downloadfile;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.Serializable;

public class DownloadConfig implements Serializable {
    private File saveFile;
    private File tempSaveFile;
    private boolean ifExistAgainDownload;
    private String fileDownloadUrl;
    private boolean useSourceName;

    protected DownloadConfig() {
    }

    public static class Builder {
        private String downloadFilePath;
        private DownloadConfig config;

        public Builder() {
            config = new DownloadConfig();
        }

        public Builder(Context context) {
            config = new DownloadConfig();
            if (context == null) {
                return;
            }
            boolean sdCardCanReadAndWrite = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            File useDownloadFile = context.getExternalFilesDir("download");
            if (sdCardCanReadAndWrite && useDownloadFile != null) {
                downloadFilePath = useDownloadFile.getAbsolutePath();
            } else {
                downloadFilePath = context.getFilesDir() + File.separator + "download";
            }
        }

        public void setSaveFile(String filePath, String fileName) {
            config.setSaveFile(filePath, fileName);
        }

        public void setSaveFile(File saveFile) {
            config.setSaveFile(saveFile);
        }

        public void setIfExistAgainDownload(boolean ifExistAgainDownload) {
            config.setIfExistAgainDownload(ifExistAgainDownload);
        }

        public void setFileDownloadUrl(String fileDownloadUrl) {
            config.setFileDownloadUrl(fileDownloadUrl);
        }

        public void setUseSourceName(boolean useSourceName) {
            config.setUseSourceName(useSourceName);
        }

        public DownloadConfig build() {
            if (config.getSaveFile() == null) {
                if (TextUtils.isEmpty(downloadFilePath)) {
                    throw new IllegalStateException("please call setSaveFile() set filePath and fileName");
                } else {
                    String fileDownloadUrl = config.getFileDownloadUrl();
                    if (TextUtils.isEmpty(fileDownloadUrl)) {
                        throw new IllegalStateException("please call setFileDownloadUrl() set downloadUrl");
                    }
                    int index = fileDownloadUrl.lastIndexOf(".");
                    String suffix = fileDownloadUrl.substring(index);
                    String fileName = MD5Coder.encode(fileDownloadUrl) + suffix;
                    if (config.isUseSourceName()) {
                        index = fileDownloadUrl.lastIndexOf("/");
                        fileName = fileDownloadUrl.substring(index);
                    }
                    config.setSaveFile(new File(downloadFilePath, fileName));
                }
            } else {
                String fileDownloadUrl = config.getFileDownloadUrl();
                if (config.isUseSourceName()&&!TextUtils.isEmpty(fileDownloadUrl)) {
                    int index = fileDownloadUrl.lastIndexOf("/");
                    String fileName = fileDownloadUrl.substring(index);
                    config.setSaveFile(new File(config.getSaveFile().getParent(), fileName));
                }
            }
            DownloadConfig downloadConfig = new DownloadConfig();

            String name = config.getSaveFile().getName();
            String substring = name.substring(0, name.lastIndexOf("."));

            downloadConfig.setTempSaveFile(new File(config.getSaveFile().getParent(),substring+".temp"));
            downloadConfig.setSaveFile(config.getSaveFile());
            downloadConfig.setFileDownloadUrl(config.getFileDownloadUrl());
            downloadConfig.setUseSourceName(config.isUseSourceName());
            downloadConfig.setIfExistAgainDownload(config.isIfExistAgainDownload());

            return downloadConfig;
        }
    }

    public File getSaveFile() {
        return saveFile;
    }

    public void setSaveFile(String filePath, String fileName) {
        setSaveFile(new File(filePath, fileName));
    }

    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }

    public File getTempSaveFile() {
        return tempSaveFile;
    }

    public void setTempSaveFile(File tempSaveFile) {
        this.tempSaveFile = tempSaveFile;
    }

    public boolean isIfExistAgainDownload() {
        return ifExistAgainDownload;
    }

    public void setIfExistAgainDownload(boolean ifExistAgainDownload) {
        this.ifExistAgainDownload = ifExistAgainDownload;
    }

    public String getFileDownloadUrl() {
        return fileDownloadUrl;
    }

    public void setFileDownloadUrl(String fileDownloadUrl) {
        this.fileDownloadUrl = fileDownloadUrl;
    }

    public boolean isUseSourceName() {
        return useSourceName;
    }

    public void setUseSourceName(boolean useSourceName) {
        this.useSourceName = useSourceName;
    }

    public boolean isExistFile(){
        return getSaveFile()!=null&&getSaveFile().exists();
    }

}
