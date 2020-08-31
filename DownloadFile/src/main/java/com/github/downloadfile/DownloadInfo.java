package com.github.downloadfile;

import android.text.TextUtils;
import android.util.Log;

import com.github.downloadfile.bean.DownloadRecord;
import com.github.downloadfile.helper.DownloadHelper;
import com.github.downloadfile.listener.DownloadListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadInfo {
    private DownloadListener downloadListener;
    private DownloadConfig downloadConfig;
    private volatile DownloadRecord downloadRecord;
    private long totalSize;
    private long localCacheSize;


    public static final int STATUS_ERROR = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_PAUSE = 3;
    public static final int STATUS_DELETE = 4;
    public static final int STATUS_PROGRESS = 5;
    public static final int STATUS_CONNECT = 6;

    private List<TaskInfo> taskInfoList = new ArrayList<>();

    private AtomicInteger status;
    private AtomicLong downloadProgress;

    public DownloadInfo(DownloadConfig config, DownloadListener listener) {
        this.downloadConfig = config;
        this.downloadListener = listener;
        downloadProgress = new AtomicLong(0);
        status = new AtomicInteger(0);
    }

    public DownloadListener getDownloadListener() {
        if (downloadListener == null) {
            downloadListener = new DownloadListener() {
                @Override
                public void onConnect(long totalSize) {

                }

                @Override
                public void onSpeed(float speedBySecond) {

                }

                @Override
                public void onProgress(long progress, long totalSize) {

                }

                @Override
                public void onSuccess(File file) {

                }

                @Override
                public void onPause() {

                }

                @Override
                public void onDelete() {

                }

                @Override
                public void onError() {

                }
            };
        }
        return downloadListener;
    }

    public void setStatus(int status) {
        this.status.set(status);
    }

    public void changeStatus(int changeStatus) {
        if (taskInfoList == null) {
            return;
        }
        setStatus(changeStatus);
        for (TaskInfo info : taskInfoList) {
            info.changeStatus(changeStatus);
        }
        switch (changeStatus) {
            case STATUS_PAUSE:
                pause();
                break;
        }
    }

    private void error() {
        int status = this.status.get();
        if (status == STATUS_ERROR) {
            return;
        }
        if (downloadConfig != null) {
            DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
            DownloadHelper.get().clearRecord(downloadConfig.getFileDownloadUrl());
        }
        setStatus(STATUS_ERROR);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onError();
            }
        });
    }

    private void pause() {
        int status = this.status.get();
        if (status == STATUS_PAUSE) {
            return;
        }
        setStatus(STATUS_PAUSE);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onPause();
            }
        });
    }

    private void delete() {
        int status = this.status.get();
        if (status == STATUS_DELETE) {
            return;
        }
        setStatus(STATUS_DELETE);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onDelete();
            }
        });
    }

    private void success(final File file) {
        int status = this.status.get();
        if (status == STATUS_SUCCESS) {
            return;
        }
        setStatus(STATUS_SUCCESS);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onSuccess(file);
            }
        });
    }

    private void connect(final long totalSize) {
        int status = this.status.get();
        if (status == STATUS_CONNECT) {
            return;
        }
        preTime = 0;
        this.totalSize = totalSize;
        downloadProgress.set(0);
        setStatus(STATUS_CONNECT);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onConnect(totalSize);
            }
        });
    }

    private long preTime;
    private long tempDownloadSize;
    private long tempTimeInterval=200;
    private synchronized void progress(final long downloadSize) {
        final long progress = downloadProgress.addAndGet(downloadSize);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                //计算网速
                if (downloadConfig.isNeedSpeed()) {
                    long nowTime = System.currentTimeMillis();
                    if (preTime <= 0) {
                        preTime = nowTime;
                    }
                    long timeInterval = nowTime - preTime;
                    if (timeInterval>=tempTimeInterval){
                        tempTimeInterval=1000;
                        float speedBySecond = tempDownloadSize * 1000f/timeInterval/1024;
                        preTime=nowTime;
                        tempDownloadSize=0;
                        getDownloadListener().onSpeed(Float.parseFloat(String.format("%.1f", speedBySecond)));
                    }else{
                        tempDownloadSize+=downloadSize;
                    }
                }
                getDownloadListener().onProgress(progress + localCacheSize, totalSize);
            }
        });
    }

    public void download(String fileUrl) {
        if (taskInfoList != null) {
            taskInfoList.clear();
        }
        if (TextUtils.isEmpty(fileUrl)) {
            error();
            return;
        }
        downloadConfig.setFileDownloadUrl(fileUrl);
        File saveFile = downloadConfig.getSaveFile();
        /*如果存在已下载完成的文件*/
        if (saveFile != null && saveFile.exists() && saveFile.isFile()) {
            if (downloadConfig.isIfExistAgainDownload()) {
                DownloadHelper.deleteFile(saveFile);
//                downloadConfig.setSaveFile(reDownloadAndRename(1));
            } else {
                long length = saveFile.length();
                connect(length);
                progress(length);
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
                    Log.i("====", "=========contentLength:" + contentLength);
                    error();
                    return;
                }
                connect(contentLength);
                /*单线程下载*/
                canSingleDownload(false);

            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                /*支持范围下载*/
                int contentLength = httpURLConnection.getContentLength();
                connect(contentLength);
                /*如果文件小于30kb，就用单线程下载*/
                if (contentLength < 30 * 1024) {
                    /*单线程下载*/
                    canSingleDownload(true);
                } else {
                    int threadNum = downloadConfig.getThreadNum();
                    /*读取本地缓存配置*/
                    downloadRecord = DownloadHelper.get().getRecord(fileUrl);
                    Log.i("=====", "=====toJson=" + downloadRecord.toJson());
                    // TODO: 2020/8/28
                    /*如果本地缓存配置有数据，但是下载的文件不存在，则删除本地配置*/
                    if (downloadConfig.getTempSaveFile() != null && !downloadConfig.getTempSaveFile().exists()) {
                        DownloadHelper.get().clearRecord(fileUrl);
                        downloadRecord = null;
                    }

                    if (downloadRecord == null || downloadRecord.getFileSize() <= 0) {
                        //如果用户手动删除了配置缓存文件，则重新下载
                        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
                        downloadRecord = new DownloadRecord(contentLength, fileUrl.hashCode() + "");
                        downloadRecord.setThreadNum(threadNum);


                    } else if (downloadRecord != null && downloadRecord.getFileSize() != contentLength) {
                        /*如果下载的文件大小和缓存的配置大小不一致，重新下载*/

                        /*删除配置缓存*/
//                        DownloadHelper.deleteFile(downloadConfig.getCacheRecordFile());
                        /*删除需要下载的文件缓存*/
                        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());

                        downloadRecord = new DownloadRecord(contentLength, fileUrl.hashCode() + "");
                        downloadRecord.setThreadNum(threadNum);
                    }

                    /*多线程下载*/
                    canMultiDownload();
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
    private void canMultiDownload() {
        /*如果重新下载，忽略之前的下载进度*/
        if (downloadConfig.isReDownload()) {
            DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
            DownloadHelper.get().clearRecord(downloadConfig.getFileDownloadUrl());
        }
        int threadNum = downloadConfig.getThreadNum();
        final List<DownloadRecord.FileRecord> fileRecordList = downloadRecord.getFileRecordList();
        setStatus(STATUS_PROGRESS);
        localCacheSize = 0;
        for (int i = 0; i < threadNum; i++) {

            final DownloadRecord.FileRecord record = fileRecordList.get(i);
            long downloadLength = record.getDownloadLength();
            if (record.getStartPoint() + downloadLength > record.getEndPoint()) {
                Log.i("=====", record.getStartPoint() + downloadLength + "===point3===" + record.getEndPoint());
                Log.i("=====", "====point4=" + downloadRecord.toJson());
            }
            if (downloadLength > 0) {
                record.setDownloadLength(record.getDownloadLength() - 1);
                downloadLength = record.getDownloadLength();
            }
            localCacheSize += downloadLength;
            final TaskInfo taskInfo = new TaskInfo(i, downloadConfig.getFileDownloadUrl(), record.getStartPoint() + downloadLength, record.getEndPoint(), downloadConfig.getTempSaveFile(), new TaskInfo.ReadStreamListener() {
                @Override
                public void readLength(long readLength) {

                    long currentProgress = record.getDownloadLength() + readLength;
                    record.setDownloadLength(currentProgress);
                    saveDownloadCacheInfo(downloadRecord);
                    progress(readLength);
                }

                @Override
                public void readComplete() {
                    checkOtherTaskInfoIsComplete();
                }

                @Override
                public void fail() {
                    checkOtherTaskInfoIsError();
                }

                @Override
                public void needDelete() {
                    checkOtherTaskInfoIsDelete();
                }
            });
            taskInfoList.add(taskInfo);
            DownloadHelper.get().getExecutorService().execute(taskInfo);
        }
    }

    private void checkOtherTaskInfoIsComplete() {
        if (taskInfoList == null) {
            return;
        }
        for (TaskInfo info : taskInfoList) {
            int taskInfoStatus = info.getCurrentStatus();
            if (taskInfoStatus != DownloadInfo.STATUS_SUCCESS) {
                return;
            }
        }

        downloadConfig.getTempSaveFile().renameTo(downloadConfig.getSaveFile());
        DownloadHelper.get().clearRecord(downloadConfig.getFileDownloadUrl());
        success(downloadConfig.getSaveFile());
    }

    /*如果外部通知下载任务需要删除，检查下载任务是否停止下载*/
    private synchronized void checkOtherTaskInfoIsDelete() {
        if (taskInfoList == null) {
            return;
        }
        for (TaskInfo info : taskInfoList) {
            int taskInfoStatus = info.getCurrentStatus();
            if (taskInfoStatus != DownloadInfo.STATUS_DELETE) {
                return;
            }
        }
        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
        DownloadHelper.deleteFile(downloadConfig.getSaveFile());
        DownloadHelper.get().clearRecord(downloadConfig.getFileDownloadUrl());
        delete();
    }

    /*如果下载任务中某个任务错误，则将其他任务改成error状态*/
    private void checkOtherTaskInfoIsError() {
        if (taskInfoList == null) {
            error();
            return;
        }
        for (TaskInfo info : taskInfoList) {
            info.changeStatus(STATUS_ERROR);
        }
        error();
    }


    /*边下载边保存当前下载进度*/
    public void saveDownloadCacheInfo(DownloadRecord downloadRecord) {
        if (downloadRecord == null) {
            return;
        }
        int status = this.status.get();
        if (status == STATUS_ERROR) {
            return;
        }
        DownloadHelper.get().saveRecord(downloadRecord);
    }

    /*可以单线程下载*/
    private void canSingleDownload(boolean canRangeDownload) {
        /*如果重新下载，忽略之前的下载进度*/
        if (downloadConfig.isReDownload()) {
//            downloadConfig.getCacheRecordFile().delete();
        }
//        if (!downloadConfig.getCacheRecordFile().getParentFile().exists()) {
//            downloadConfig.getCacheRecordFile().getParentFile().mkdirs();
//        }
        /*读取本地缓存配置*/
        downloadRecord = DownloadHelper.get().getRecord(downloadConfig.getFileDownloadUrl());
        if (downloadRecord == null || downloadRecord.getFileSize() <= 0) {
            /*重新下载*/
            File tempSaveFile = downloadConfig.getTempSaveFile();
            if (tempSaveFile != null) {
                tempSaveFile.delete();
            }
            downloadRecord = new DownloadRecord(1, downloadConfig.getFileDownloadUrl().hashCode() + "");
            DownloadRecord.FileRecord record = new DownloadRecord.FileRecord();
            downloadRecord.addFileRecordList(record);
        }
        /*如果不可以范围下载*/
        if (!canRangeDownload) {
//            downloadRecord.getSingleDownloadRecord().setStartPoint(0);
        }
        int startPoint = 0;//downloadRecord.getSingleDownloadRecord().getDownloadLength();
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
            URL url = new URL(downloadConfig.getFileDownloadUrl());
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
//                    downloadRecord.getSingleDownloadRecord().setDownloadLength(downloadRecord.getSingleDownloadRecord().getDownloadLength() + len);

                    //下载的进度同时缓存至本地
//                    out = new ObjectOutputStream(new FileOutputStream(downloadConfig.getCacheRecordFile()));
//                    out.writeObject(downloadRecord);

                }

                downloadConfig.getTempSaveFile().renameTo(downloadConfig.getSaveFile());
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


}
