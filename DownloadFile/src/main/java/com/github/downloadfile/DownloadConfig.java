package com.github.downloadfile;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

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
    private int threadNum=3;

    protected DownloadConfig() {
    }

    public static class Builder {
        private String downloadFilePath;
        private DownloadConfig config;
        private Context context;
        public Builder( ) {
            context=DownloadManager.getContext();
            config = new DownloadConfig();
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
        public void setThreadNum(int threadNum) {
            if(threadNum<0){
                threadNum=1;
            }
            config.setThreadNum(threadNum);
        }

        public void setNeedSpeed(boolean needSpeed) {
            this.config.setNeedSpeed(needSpeed);
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
                    fileDownloadUrl=fileDownloadUrl.split("\\?")[0];
                    int index = fileDownloadUrl.lastIndexOf(".");
                    String suffix = fileDownloadUrl.substring(index);
                    String fileName = MD5Coder.encode(fileDownloadUrl).substring(8, 24) + suffix;
                    if (config.isUseSourceName()) {
                        index = fileDownloadUrl.lastIndexOf("/");
                        fileName = fileDownloadUrl.substring(index);
                    }
                    config.setSaveFile(new File(downloadFilePath, fileName));
                }
            } else {
                String fileDownloadUrl = config.getFileDownloadUrl();
                fileDownloadUrl=fileDownloadUrl.split("\\?")[0];
                if (config.isUseSourceName()&&!TextUtils.isEmpty(fileDownloadUrl)) {
                    int index = fileDownloadUrl.lastIndexOf("/");
                    String fileName = fileDownloadUrl.substring(index);
                    config.setSaveFile(new File(config.getSaveFile().getParent(), fileName));
                }
            }
            DownloadConfig downloadConfig = new DownloadConfig();

            downloadConfig.setSaveFile(config.getSaveFile());
            downloadConfig.setFileDownloadUrl(config.getFileDownloadUrl());
            downloadConfig.setUseSourceName(config.isUseSourceName());
            downloadConfig.setNeedSpeed(config.isNeedSpeed());
            downloadConfig.setThreadNum(config.getThreadNum());
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
        if(saveFile!=null&&!saveFile.getParentFile().exists()){
            saveFile.getParentFile().mkdirs();
        }
        this.saveFile = saveFile;
        createTempSaveFileBySaveFile(saveFile);
        createRecordFileBySaveFile(saveFile);
    }

    public File getTempSaveFile() {
        return tempSaveFile;
    }


    public void createTempSaveFileBySaveFile(File saveFile) {
        String name = saveFile.getName();
        String substring = name.substring(0, name.lastIndexOf("."));
        this.tempSaveFile = new File(saveFile.getParent(),substring+".temp");
    }
    public void createRecordFileBySaveFile(File saveFile) {
        String name = saveFile.getName();
        String substring = name.substring(0, name.lastIndexOf("."));
    }

    public boolean isReDownload() {
        return reDownload;
    }

    public void setReDownload(boolean reDownload) {
        this.reDownload = reDownload;
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

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        if(threadNum<0){
            threadNum=1;
        }
        this.threadNum = threadNum;
    }

    public boolean isNeedSpeed() {
        return needSpeed;
    }

    public void setNeedSpeed(boolean needSpeed) {
        this.needSpeed = needSpeed;
    }
}
