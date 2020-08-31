package com.github.downloadfile;

import android.text.TextUtils;
import android.util.Log;

import com.github.downloadfile.helper.DownloadHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class TaskInfo implements Runnable {
    public interface ReadStreamListener {
        void readLength(long readLength);

        void readComplete();

        void fail();

        void needDelete();
    }

    private String fileUrl;
    private long startPoint;
    private long endPoint;
    private File saveFile;
    private ReadStreamListener downloadListener;

    private int currentStatus;


    public TaskInfo(String fileUrl, long startPoint, long endPoint, File saveFile, ReadStreamListener downloadListener) {
        this.fileUrl = fileUrl;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.saveFile = saveFile;
        this.downloadListener = downloadListener;
    }

    /*接收到外部的暂停或者删除或者错误通知时*/
    public void changeStatus(int status) {
        if (currentStatus == status || downloadListener == null) {
            return;
        }
        if (status == DownloadInfo.STATUS_DELETE) {
            if (currentStatus != DownloadInfo.STATUS_PROGRESS) {
                downloadListener.needDelete();
            }
        }
        setCurrentStatus(status);
    }

    private void setCurrentStatus(int currentStatus) {
        this.currentStatus = currentStatus;
    }

    public int getCurrentStatus() {
        return currentStatus;
    }

    @Override
    public void run() {
        setCurrentStatus(DownloadInfo.STATUS_PROGRESS);
        startMultiDownload(fileUrl, startPoint, endPoint, saveFile);
    }

    private void startMultiDownload(String fileUrl, long startPoint, long endPoint, File saveFile) {
        if (downloadListener == null) {
            setCurrentStatus(DownloadInfo.STATUS_ERROR);
            return;
        }
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        // 随机访问文件，可以指定断点续传的起始位置
        BufferedInputStream bis = null;
        RandomAccessFile randomAccessFile = null;
        if (TextUtils.isEmpty(fileUrl) || saveFile == null) {
            setCurrentStatus(DownloadInfo.STATUS_ERROR);
            Log.i("=====", "=====fail333");
            downloadListener.fail();
            return;
        }
        try {
            URL url = new URL(fileUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            Log.i("=====", startPoint + "===point2===" + endPoint);
            if(endPoint!=0){
                httpURLConnection.setRequestProperty("Range", "bytes=" + startPoint + "-" + endPoint);
            }
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_PARTIAL||responseCode == HttpURLConnection.HTTP_OK) {
                int contentLength = httpURLConnection.getContentLength();
                if (contentLength == 0) {
                    setCurrentStatus(DownloadInfo.STATUS_SUCCESS);
                    downloadListener.readComplete();
                    return;
                }
                inputStream = httpURLConnection.getInputStream();

                byte[] buff = new byte[2048 * 15];
                int len = 0;
                bis = new BufferedInputStream(inputStream);
                randomAccessFile = new RandomAccessFile(saveFile, "rwd");
                randomAccessFile.seek(startPoint);
                while ((len = bis.read(buff)) != -1) {
                    if (currentStatus == DownloadInfo.STATUS_ERROR || currentStatus == DownloadInfo.STATUS_PAUSE) {
                        return;
                    }
                    /*外部通知删除时，回调给外部*/
                    if (currentStatus == DownloadInfo.STATUS_DELETE) {
                        downloadListener.needDelete();
                        return;
                    }

                    randomAccessFile.write(buff, 0, len);
                    downloadListener.readLength(len);

                }
                setCurrentStatus(DownloadInfo.STATUS_SUCCESS);
                downloadListener.readComplete();
            } else {
                setCurrentStatus(DownloadInfo.STATUS_ERROR);
                Log.i("=====", startPoint + "===" + endPoint + "=====fail11111111==" + responseCode);
                downloadListener.fail();
            }
        } catch (Exception e) {
            e.printStackTrace();
            setCurrentStatus(DownloadInfo.STATUS_ERROR);
            Log.i("=====", "=====fail22222222");
            downloadListener.fail();
        } finally {
            DownloadHelper.close(randomAccessFile);
            DownloadHelper.close(bis);
            DownloadHelper.close(inputStream);
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }
}
