package com.github.downloadfile;

import android.text.TextUtils;

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
        boolean notNeedRead();
        void fail();
    }
    private String fileUrl;
    private long startPoint;
    private long endPoint;
    private File saveFile;
    private ReadStreamListener downloadListener;

    public TaskInfo(String fileUrl, long startPoint, long endPoint, File saveFile, ReadStreamListener downloadListener) {
        this.fileUrl = fileUrl;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.saveFile = saveFile;
        this.downloadListener = downloadListener;
    }

    @Override
    public void run() {
        startMultiDownload(fileUrl, startPoint, endPoint, saveFile, downloadListener);
    }

    private void startMultiDownload(String fileUrl, long startPoint, long endPoint, File saveFile, ReadStreamListener downloadListener) {
        if (downloadListener == null) {
            return;
        }
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        // 随机访问文件，可以指定断点续传的起始位置
        BufferedInputStream bis = null;
        RandomAccessFile randomAccessFile = null;
        if (TextUtils.isEmpty(fileUrl) || saveFile == null) {
            downloadListener.fail();
            return;
        }
        try {
            URL url = new URL(fileUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range", "bytes=" + startPoint + "-" + endPoint);
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                inputStream = httpURLConnection.getInputStream();

                byte[] buff = new byte[2048 * 15];
                int len = 0;
                bis = new BufferedInputStream(inputStream);
                randomAccessFile = new RandomAccessFile(saveFile, "rwd");
                randomAccessFile.seek(startPoint);
                long downloadLength;
                while ((len = bis.read(buff)) != -1) {
                    randomAccessFile.write(buff, 0, len);
//                    downloadLength = record.getDownloadLength();
//                    record.setDownloadLength(downloadLength + len);
                    downloadListener.readLength(len);

                    boolean notNeedRead = downloadListener.notNeedRead();
                    if (notNeedRead) {
                        return;
                    }
//                    saveDownloadCacheInfo(downloadRecord);

                    /*    *//*如果其他下载任务出现异常*//*
                    if(statusListener.getStatus()==DownloadInfo.status_error){
                        return;
                    }
                    *//*如果外部调用暂停方法*//*
                    if (statusListener.getStatus()==DownloadInfo.status_pause) {
                        int num = multiPauseNum.incrementAndGet();
                        if (num == downloadConfig.getThreadNum()) {
                            pause();
                        }
                        return;
                    }
                    *//*如果外部调用删除方法*//*
                    if (statusListener.getStatus()==DownloadInfo.status_delete) {
                        int num = multiDeleteNum.incrementAndGet();
                        if (num == downloadConfig.getThreadNum()) {
                            delete();
                        }
                        return;
                    }*/

                }
               /* int num = multiCompleteNum.incrementAndGet();
                if (num == downloadConfig.getThreadNum()) {
                    long endTime = System.currentTimeMillis();
                    downloadConfig.getTempSaveFile().renameTo(downloadConfig.getSaveFile());
                    DownloadHelper.get().clearRecord(downloadRecord.getUniqueId());
                    status=status_success;
                    success(downloadConfig.getSaveFile());
                }*/
            } else {
                downloadListener.fail();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
