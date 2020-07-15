package com.github.downloadfile;

import android.text.TextUtils;

import com.github.downloadfile.bean.DownloadRecord;
import com.github.downloadfile.helper.DownloadHelper;
import com.github.downloadfile.listener.DownloadListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DownloadInfo implements Runnable{
    private int fileSize;
    private DownloadListener downloadListener;
    private DownloadConfig downloadConfig;
    private DownloadRecord downloadRecord;

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
        downloadConfig.setDownloadUrl(fileUrl);
        File saveFile = downloadConfig.getSaveFile();
        /*如果存在已下载完成的文件*/
        if (saveFile != null && saveFile.exists() && saveFile.isFile()) {
            if (downloadConfig.isIfExistAgainDownload()) {
                downloadConfig.setSaveFile(reDownloadAndRename(1));
            } else {
                long length = saveFile.length();
                connect((int) length);
                progress((int) length);
                success(saveFile);
                return;
            }
        }
        HttpURLConnection httpURLConnection = null;
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
                fileSize = contentLength;
                /*单线程下载*/
                canSingleDownload(false);

            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                /*支持范围下载*/
                int contentLength = httpURLConnection.getContentLength();
                fileSize = contentLength;
                /*如果文件小于30kb，就用单线程下载*/
                if (contentLength < 30 * 1024) {
                    /*单线程下载*/
                    canSingleDownload(true);
                } else {
                    /*多线程下载*/
                    canMultiDownload(fileSize);
//                    canSingleDownload(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }

    }

    /*重新下载时重命名*/
    private File reDownloadAndRename(int reNum) {
        File saveFile = downloadConfig.getSaveFile();
        String parent = saveFile.getParent();
        String name = saveFile.getName();
        String newName = name.replace(".", "(" + reNum + ").");
        File newFile = new File(parent, newName);
        if (!newFile.exists()) {
            return newFile;
        } else {
            return reDownloadAndRename(reNum + 1);
        }
    }

    /*可以多线程下载*/
    private void canMultiDownload(int contentLength) {
        /*如果重新下载，忽略之前的下载进度*/
        if (downloadConfig.isReDownload()) {
            downloadConfig.getCacheRecordFile().delete();
        }

        int threadNum = downloadConfig.getThreadNum();
        int average= contentLength / threadNum;

        /*读取本地缓存配置*/
        downloadRecord = SerializeUtils.toObjectSyn(downloadConfig.getCacheRecordFile());
        if (downloadRecord == null) {
            /*重新下载*/
            File tempSaveFile = downloadConfig.getTempSaveFile();
            if (tempSaveFile != null) {
                tempSaveFile.delete();
            }
            downloadRecord = new DownloadRecord();
            for (int i = 0; i <threadNum; i++) {
                int start=average*i;
                int end;
                if(i==(threadNum-1)){
                    end=contentLength;
                }else{
                    end=start+average-1;
                }
                DownloadRecord.FileRecord record = new DownloadRecord.FileRecord();
                record.setStartPoint(start);
                record.setEndPoint(end);
                downloadRecord.addFileRecordList(record);
            }
        }

        for (int i = 0; i <threadNum; i++) {

        }
    }

    @Override
    public void run() {
//        startMultiDownload();
    }

    private void startMultiDownload(int index,int startPoint,int endPoint) {
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        // 随机访问文件，可以指定断点续传的起始位置
        BufferedInputStream bis = null;
        RandomAccessFile randomAccessFile = null;

        ObjectOutputStream out = null;

        DownloadRecord.FileRecord record = downloadRecord.getFileRecordList().get(index);
        if(randomAccessFile==null){
            error();
            return;
        }
        try {
            URL url = new URL(downloadConfig.getDownloadUrl());
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range", "bytes=" + startPoint + "-"+endPoint);
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                inputStream = httpURLConnection.getInputStream();

                File targetFile = downloadConfig.getTempSaveFile();
                byte[] buff = new byte[2048];
                int len = 0;
                bis = new BufferedInputStream(inputStream);
                randomAccessFile = new RandomAccessFile(targetFile, "rwd");
                randomAccessFile.seek(record.getStartPoint()+record.getDownloadLength()+1);
                while ((len = bis.read(buff)) != -1) {
                    randomAccessFile.write(buff, 0, len);
                    record.setDownloadLength(record.getDownloadLength() + len);

                    //下载的进度同时缓存至本地
                    out = new ObjectOutputStream(new FileOutputStream(downloadConfig.getCacheRecordFile()));
                    out.writeObject(downloadRecord);

                }

                downloadConfig.getTempSaveFile().renameTo(downloadConfig.getSaveFile());
                downloadConfig.getCacheRecordFile().delete();
                success(downloadConfig.getSaveFile());
            } else {
                error();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DownloadHelper.close(randomAccessFile);
            DownloadHelper.close(bis);
            DownloadHelper.close(inputStream);

            DownloadHelper.flush(out);
            DownloadHelper.close(out);
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    /*可以单线程下载*/
    private void canSingleDownload(boolean canRangeDownload) {
        /*如果重新下载，忽略之前的下载进度*/
        if (downloadConfig.isReDownload()) {
            downloadConfig.getCacheRecordFile().delete();
        }
        /*读取本地缓存配置*/
        downloadRecord = SerializeUtils.toObjectSyn(downloadConfig.getCacheRecordFile());
        if (downloadRecord == null) {
            /*重新下载*/
            File tempSaveFile = downloadConfig.getTempSaveFile();
            if (tempSaveFile != null) {
                tempSaveFile.delete();
            }
            downloadRecord = new DownloadRecord();
            DownloadRecord.FileRecord record = new DownloadRecord.FileRecord();
            downloadRecord.addFileRecordList(record);
        }
        /*如果不可以范围下载*/
        if (!canRangeDownload) {
            downloadRecord.getSingleDownloadRecord().setStartPoint(0);
        }
        int startPoint = downloadRecord.getSingleDownloadRecord().getDownloadLength();
        int nextStartPoint = 0;
        if (startPoint != 0) {
            nextStartPoint = startPoint + 1;
        }
        startSingleDownload(nextStartPoint);


    }

    private void startSingleDownload(int startPoint) {
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        // 随机访问文件，可以指定断点续传的起始位置
        BufferedInputStream bis = null;
        RandomAccessFile randomAccessFile = null;

        ObjectOutputStream out = null;
        try {
            URL url = new URL(downloadConfig.getDownloadUrl());
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range", "bytes=" + startPoint + "-");
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                inputStream = httpURLConnection.getInputStream();

                File targetFile = downloadConfig.getTempSaveFile();
                byte[] buff = new byte[2048];
                int len = 0;
                bis = new BufferedInputStream(inputStream);
                randomAccessFile = new RandomAccessFile(targetFile, "rwd");
                randomAccessFile.seek(startPoint);
                while ((len = bis.read(buff)) != -1) {
                    randomAccessFile.write(buff, 0, len);
                    downloadRecord.getSingleDownloadRecord().setDownloadLength(downloadRecord.getSingleDownloadRecord().getDownloadLength() + len);

                    //下载的进度同时缓存至本地
                    out = new ObjectOutputStream(new FileOutputStream(downloadConfig.getCacheRecordFile()));
                    out.writeObject(downloadRecord);

                }

                downloadConfig.getTempSaveFile().renameTo(downloadConfig.getSaveFile());
                downloadConfig.getCacheRecordFile().delete();
                success(downloadConfig.getSaveFile());
            } else {
                error();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DownloadHelper.close(randomAccessFile);
            DownloadHelper.close(bis);
            DownloadHelper.close(inputStream);

            DownloadHelper.flush(out);
            DownloadHelper.close(out);
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    private void writeFile(int startPoint, File targetFile, InputStream inputStream) {
        byte[] buff = new byte[2048];
        int len = 0;
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        // 随机访问文件，可以指定断点续传的起始位置
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(targetFile, "rwd");
            randomAccessFile.seek(startPoint);
            while ((len = bis.read(buff)) != -1) {
                randomAccessFile.write(buff, 0, len);
                downloadRecord.getSingleDownloadRecord().setDownloadLength(downloadRecord.getSingleDownloadRecord().getDownloadLength() + len);
                SerializeUtils.toSerialSyn(downloadRecord, downloadConfig.getCacheRecordFile());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DownloadHelper.close(randomAccessFile);
            DownloadHelper.close(bis);
        }
    }

}
