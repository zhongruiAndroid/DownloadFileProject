package com.github.downloadfile;

import android.text.TextUtils;

import com.github.downloadfile.helper.DownloadHelper;
import com.github.downloadfile.listener.DownloadListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadInfo {
    private DownloadListener downloadListener;
    private DownloadConfig downloadConfig;

    public DownloadInfo(DownloadConfig config, DownloadListener listener) {
        this.downloadConfig = config;
        this.downloadListener = listener;
    }

    public DownloadListener getDownloadListener() {
        if (downloadListener == null) {
            downloadListener = new DownloadListener() {
                @Override
                public void onConnect(long fileSizeKB) {

                }

                @Override
                public void onProgress(long downloadSizeKB) {

                }

                @Override
                public void onSuccess(File file) {

                }

                @Override
                public void onPause() {

                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onError() {

                }
            };
        }
        return downloadListener;
    }

    private void error() {
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onError();
            }
        });
    }

    private void success(final File file) {
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onSuccess(file);
            }
        });
    }
    private void connect(final int downloadSizeKB) {
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onConnect(downloadSizeKB);
            }
        });
    }
    private void progress(final int downloadSizeKB) {
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onProgress(downloadSizeKB);
            }
        });
    }

    public void download(String fileUrl) {
        if (TextUtils.isEmpty(fileUrl)) {
            error();
            return;
        }
        File saveFile = downloadConfig.getSaveFile();
        /*如果存在已下载完成的文件*/
        if (saveFile != null && saveFile.exists()&&saveFile.isFile()) {
            if(downloadConfig.isIfExistAgainDownload()){
                downloadConfig.setSaveFile(reDownloadAndRename(1));
            }else{
                long length = saveFile.length();
                connect((int) length);
                progress((int) length);
                success(saveFile);
                return;
            }
        }
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        try {
            URL url = new URL(fileUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range", "bytes=" + 0 + "-");
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                /*不支持范围下载*/
                int contentLength = httpURLConnection.getContentLength();
                if (contentLength <= 0) {
                    error();
                    return;
                }
                inputStream = httpURLConnection.getInputStream();
                /*单线程下载*/
                startSingleDownload(inputStream);

            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                /*支持范围下载*/
                int contentLength = httpURLConnection.getContentLength();
                /*如果文件小于30kb，就用单线程下载*/
                if (contentLength < 30 * 1024) {
                    /*单线程下载*/
                    startSingleDownload(inputStream);
                } else {
                    /*多线程下载*/
                    startMultiDownload(fileUrl, contentLength);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DownloadHelper.close(inputStream);
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }

    }

    /*重新下载和重命名*/
    private File reDownloadAndRename(int reNum) {
        File saveFile = downloadConfig.getSaveFile();
        String parent = saveFile.getParent();
        String name = saveFile.getName();
        String newName = name.replace(".", "(" + reNum + ").");
        File newFile=new File(parent,newName);
        if(!newFile.exists()){
            return newFile;
        }else{
            return reDownloadAndRename(reNum+1);
        }
    }

    private void startMultiDownload(String fileUrl, int contentLength) {

    }

    private void startSingleDownload(InputStream inputStream) {
        /*读取本地缓存配置*/

        File tempSaveFile = downloadConfig.getTempSaveFile();
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        // 随机访问文件，可以指定断点续传的起始位置
        RandomAccessFile  randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(tempSaveFile, "rwd");

            /*randomAccessFile.seek(startsPoint);
            while ((len = bis.read(buff)) != -1) {
                randomAccessFile.write(buff, 0, len);
            }*/
            tempSaveFile.renameTo(downloadConfig.getSaveFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            DownloadHelper.close(randomAccessFile);
            DownloadHelper.close(bis);
        }
    }
}
