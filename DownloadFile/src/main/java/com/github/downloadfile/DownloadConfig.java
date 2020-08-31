package com.github.downloadfile;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.Serializable;

public class DownloadConfig implements Serializable {
    /*下次成功的文件*/
    private File saveFile;
    /*下载中的文件*/
    private File tempSaveFile;
    /*重新下载，忽略之前下载的进度*/
    private boolean reDownload;
    private boolean ifExistAgainDownload;
    /*下载地址*/
    private String fileDownloadUrl;
    private boolean useSourceName;
    private boolean needSpeed;

    /*单个任务多线程下载数量*/
    private int threadNum = 2;


    protected DownloadConfig(Builder builder) {
        if (builder.saveFile == null) {
            if (TextUtils.isEmpty(builder.downloadFileSavePath)) {
                throw new IllegalStateException("please call setSaveFile() set filePath and fileName");
            } else {
                String fileDownloadUrl = builder.fileDownloadUrl;
                if (TextUtils.isEmpty(fileDownloadUrl)) {
                    throw new IllegalStateException("please call setFileDownloadUrl() set downloadUrl");
                }
                fileDownloadUrl = fileDownloadUrl.split("\\?")[0];
                int index = fileDownloadUrl.lastIndexOf(".");
                /*通过url获取文件后缀*/
                String suffix = fileDownloadUrl.substring(index);
                String fileName = MD5Coder.encode(fileDownloadUrl).substring(8, 24) + suffix;
                if (builder.useSourceName) {
                    index = fileDownloadUrl.lastIndexOf("/");
                    fileName = fileDownloadUrl.substring(index);
                }
                Log.i("=====","====="+fileName);
                builder.setSaveFile(new File(builder.downloadFileSavePath, fileName));
            }
        } else {
            String fileDownloadUrl = builder.fileDownloadUrl;
            fileDownloadUrl = fileDownloadUrl.split("\\?")[0];
            if (builder.useSourceName && !TextUtils.isEmpty(fileDownloadUrl)) {
                int index = fileDownloadUrl.lastIndexOf("/");
                String fileName = fileDownloadUrl.substring(index);
                if(builder.saveFile.isFile()){
                    builder.setSaveFile(new File(builder.saveFile.getParent(), fileName));
                }else{
                    builder.setSaveFile(new File(builder.saveFile, fileName));
                }
            }
        }

        this.saveFile = builder.saveFile;
        this.tempSaveFile = builder.tempSaveFile;
        this.reDownload = builder.reDownload;
        this.ifExistAgainDownload = builder.ifExistAgainDownload;
        this.fileDownloadUrl = builder.fileDownloadUrl;
        this.useSourceName = builder.useSourceName;
        this.needSpeed = builder.needSpeed;
        this.threadNum = builder.threadNum;
    }

    public static class Builder {
        private Context context;
        private String downloadFileSavePath;


        /*下次成功的文件*/
        private File saveFile;
        /*下载中的文件*/
        private File tempSaveFile;
        /*重新下载，忽略之前下载的进度*/
        private boolean reDownload;
        private boolean ifExistAgainDownload;
        /*下载地址*/
        private String fileDownloadUrl;
        private boolean useSourceName;
        private boolean needSpeed;

        /*单个任务多线程下载数量*/
        private int threadNum = 3;

        public Builder() {
            context = DownloadManager.getContext();
            boolean sdCardCanReadAndWrite = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            File useDownloadFile = context.getExternalFilesDir("download");
            if (sdCardCanReadAndWrite && useDownloadFile != null) {
                downloadFileSavePath = useDownloadFile.getAbsolutePath();
            } else {
                downloadFileSavePath = context.getFilesDir() + File.separator + "download";
            }
        }

        public void setSaveFile(File saveFile) {
            if (saveFile != null && !saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            this.saveFile = saveFile;
            createTempSaveFileBySaveFile(saveFile);
        }

        public void setSaveFile(String filePath, String fileName) {
            setSaveFile(new File(filePath, fileName));
        }

        public void createTempSaveFileBySaveFile(File saveFile) {
            String name = saveFile.getName();
            String substring = name.substring(0, name.lastIndexOf("."));
            this.tempSaveFile = new File(saveFile.getParent(), substring + ".temp");
        }

        public void setIfExistAgainDownload(boolean ifExistAgainDownload) {
            this.ifExistAgainDownload = ifExistAgainDownload;
        }

        public void setFileDownloadUrl(String fileDownloadUrl) {
            this.fileDownloadUrl = fileDownloadUrl;
        }

        public void setUseSourceName(boolean useSourceName) {
            this.useSourceName = useSourceName;
        }

        public void setThreadNum(int threadNum) {
            if (threadNum <= 0) {
                threadNum = 1;
            }
            this.threadNum = threadNum;
        }

        public void setNeedSpeed(boolean needSpeed) {
            this.needSpeed = needSpeed;
        }

        public void setReDownload(boolean reDownload) {
            this.reDownload = reDownload;
        }

        public DownloadConfig build() {
            DownloadConfig downloadConfig = new DownloadConfig(this);
            return downloadConfig;
        }
    }

    public File getSaveFile() {
        return saveFile;
    }

    public File getTempSaveFile() {
        return tempSaveFile;
    }

    public boolean isReDownload() {
        return reDownload;
    }

    public boolean isIfExistAgainDownload() {
        return ifExistAgainDownload;
    }

    public String getFileDownloadUrl() {
        return fileDownloadUrl;
    }

    public boolean isUseSourceName() {
        return useSourceName;
    }

    public boolean isNeedSpeed() {
        return needSpeed;
    }

    public int getThreadNum() {
        if (threadNum <= 0) {
            threadNum = 1;
        }
        return threadNum;
    }
}
