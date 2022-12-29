package com.github.downloadfile;

import android.text.TextUtils;

import com.github.downloadfile.bean.DownloadRecord;
import com.github.downloadfile.helper.DownloadHelper;
import com.github.downloadfile.listener.FileDownloadListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadInfo {
    private FileDownloadListener downloadListener;
    private DownloadConfig downloadConfig;
    private volatile DownloadRecord downloadRecord;
    /*下载文件的总大小*/
    private long totalSize;
    /*已下载的缓存大小，这个大小是从缓存记录读取，不是从缓存文件*/
    private long localCacheSize;


    public static final int STATUS_ERROR = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_PAUSE = 3;
    public static final int STATUS_DELETE = 4;
    public static final int STATUS_PROGRESS = 5;
    public static final int STATUS_CONNECT = 6;
    public static final int STATUS_REQUEST = 7;

    private List<TaskInfo> taskInfoList = new ArrayList<>();

    private AtomicInteger status;
    private AtomicLong downloadProgress;

    public DownloadInfo(DownloadConfig config, FileDownloadListener listener) {
        this.downloadConfig = config;
        this.downloadListener = listener;
        status = new AtomicInteger(0);
        downloadProgress = new AtomicLong(0);
    }

    private AppStateUtils.AppStateChangeListener appStateChangeListener = new AppStateUtils.AppStateChangeListener() {
        @Override
        public void onStateChange(boolean intoFront) {
            notifySaveRecord();
        }
    };

    private void setAppStateChangeListener() {
        AppStateUtils.get().addAppStateChangeListener(this, appStateChangeListener);
    }

    private void removeAppStateChangeListener() {
        AppStateUtils.get().removeAppStateChangeListener(this);
    }

    public FileDownloadListener getDownloadListener() {
        if (downloadListener == null) {
            downloadListener = new FileDownloadListener() {
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

    private void setStatus(int status) {
        this.status.set(status);
    }

    public int getStatus() {
        return status.get();
    }

    public void pauseDownload() {
        changeStatus(STATUS_PAUSE);
    }

    public void deleteDownload() {
        deleteDownload(false);
    }

    public void deleteDownload(boolean f) {
        changeStatus(STATUS_DELETE);
    }

    private void changeStatus(int changeStatus) {
        if (taskInfoList == null) {
            return;
        }
        if (getStatus() == STATUS_SUCCESS) {
            return;
        }
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
        error(false);
    }

    private void error(boolean notClearCache) {
        if (downloadConfig != null && !notClearCache) {
            DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
            DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
        } else {
            saveDownloadCacheInfo(downloadRecord);
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
        int status = getStatus();
        if (status == STATUS_PAUSE) {
            return;
        }
        setStatus(STATUS_PAUSE);
        /*手动暂停时把内存的缓存信息保存至本地*/
        saveDownloadCacheInfo(downloadRecord);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onPause();
            }
        });
    }

    private void delete() {
        int status = getStatus();
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
        int status = getStatus();
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
        int status = getStatus();
        if (status == STATUS_CONNECT) {
            return;
        }
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
    private AtomicLong tempDownloadSize;
    private long tempTimeInterval = 200;

    private void reset() {
        if (taskInfoList != null) {
            taskInfoList.clear();
        } else {
            taskInfoList = new ArrayList<>();
        }
        preTime = 0;
        tempDownloadSize = new AtomicLong(0);
        localCacheSize = 0;
    }

    private void progress(final long downloadSize) {
        if (getStatus() != STATUS_PROGRESS) {
            return;
        }
        final long progress = downloadProgress.addAndGet(downloadSize);
        //计算网速
        if (downloadConfig.isNeedSpeed()) {
            long nowTime = System.currentTimeMillis();
            if (preTime <= 0) {
                tempDownloadSize.set(0);
                preTime = nowTime;
            }
            long timeInterval = nowTime - preTime;
            if (timeInterval >= tempTimeInterval) {
                tempTimeInterval = 1000;
                final float speedBySecond = tempDownloadSize.get() * 1000f / timeInterval;
                preTime = nowTime;
                tempDownloadSize.set(0);
                DownloadHelper.get().getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        getDownloadListener().onSpeed(Float.parseFloat(String.format("%.1f", speedBySecond)));
                    }
                });
            } else {
                tempDownloadSize.addAndGet(downloadSize);
            }
        }
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onProgress(progress + localCacheSize, totalSize);
            }
        });
    }

    private long getContentLength(HttpURLConnection httpURLConnection) {
        String value = httpURLConnection.getHeaderField("content-length");
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return -1;
        }
    }

    public void download() {
        if (downloadConfig == null) {
            getDownloadListener().onError();
        }
        String fileUrl = downloadConfig.getFileDownloadUrl();
        if (TextUtils.isEmpty(fileUrl)) {
            getDownloadListener().onError();
            return;
        }
        if (getStatus() == STATUS_CONNECT || getStatus() == STATUS_PROGRESS || getStatus() == STATUS_REQUEST) {
            return;
        }
        DownloadHelper.get().getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                downloadByChildThread();
            }
        });
    }

    private void downloadByChildThread() {
        setAppStateChangeListener();
        reset();
        /*下载完成后需要保存的文件*/
        File saveFile = downloadConfig.getSaveFile();
        if (!saveFile.getParentFile().exists()) {
            saveFile.getParentFile().mkdirs();
        }
        /*如果存在已下载完成的文件*/
        if (saveFile != null && saveFile.exists() && saveFile.isFile()) {
            if (downloadConfig.isIfExistAgainDownload()) {
                DownloadHelper.deleteFile(saveFile);
//                downloadConfig.setSaveFile(DownloadHelper.reDownloadAndRename(saveFile, 1));
            } else {
                /*如果本地已存在下载的文件，直接返回*/
                long length = saveFile.length();
                connect(length);
                progress(length);
                success(saveFile);
                return;
            }
        }
        /*先判断内存是否存在数据，不存在再读取本地缓存配置,用于[下载-暂停-再下载]流程*/
        if (DownloadRecord.isEmpty(downloadRecord)) {
            downloadRecord = DownloadHelper.get().getRecord(downloadConfig.getDownloadSPName(), getDownloadConfig().getUnionId());
        }

        /*如果没有下载记录，那么需要删除之前已经下载的临时文件*/
        /*或者如果需要重新下载，忽略之前的下载进度*/
        if (DownloadRecord.isEmpty(downloadRecord) || (downloadConfig != null && downloadConfig.isReDownload())) {
            DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
            DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
            downloadRecord = null;

        }
        /*如果本地有下载记录，但是下载一部分的本地文件已经不存在了*/
        if (downloadRecord != null && downloadRecord.hasDownloadRecord()) {
            if (downloadConfig.getTempSaveFile() != null && !downloadConfig.getTempSaveFile().exists()) {
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
                downloadRecord = null;
            }
        }

        HttpURLConnection httpURLConnection = null;
        try {
            setStatus(STATUS_REQUEST);
            URL url = new URL(downloadConfig.getFileDownloadUrl());
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range", "bytes=" + 0 + "-");
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            String eTag = httpURLConnection.getHeaderField("ETag");
            String lastModified = httpURLConnection.getHeaderField("Last-Modified");
            long contentLength = getContentLength(httpURLConnection);
            if (contentLength < 0) {
                DownloadHelper.close(httpURLConnection);
                /*有可能状态码=200，但是内容长度为null*/
                error();
                return;
            }
            connect(contentLength);
            if (!DownloadHelper.hasFreeSpace(FileDownloadManager.getContext(), contentLength)) {
                DownloadHelper.close(httpURLConnection);
                //储存空间不足
                error();
                return;
            }
            /*如果首次下载*/
            if (downloadRecord == null || downloadRecord.getFileSize() <= 0) {
                downloadRecord = new DownloadRecord(contentLength, downloadConfig.getThreadNum());
                downloadRecord.setFileSize(contentLength);
                downloadRecord.setDownloadUrl(downloadConfig.getFileDownloadUrl());
                downloadRecord.setSaveFilePath(downloadConfig.getSaveFile().getAbsolutePath());
                downloadRecord.setUniqueId(downloadConfig.getUnionId());
            }
            /*上次请求的eTag和lastModified,如果和这次请求返回的不一样，则从头开始下载*/
            String preETag = downloadRecord.geteTag();
            String preLastModified = downloadRecord.getLastModified();
            if (!TextUtils.isEmpty(eTag) && !TextUtils.isEmpty(preETag) && !TextUtils.equals(eTag, preETag)) {
                /*文件被修改*/
                removeAppStateChangeListener();
                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
                downloadRecord = null;

                /*因为需要自己调用自己，所以这里提前手动关闭连接*/
                DownloadHelper.close(httpURLConnection);

                downloadByChildThread();
                return;
            } else if (!TextUtils.isEmpty(lastModified) && !TextUtils.isEmpty(preLastModified) && !TextUtils.equals(lastModified, preLastModified)) {
                /*文件被修改*/
                removeAppStateChangeListener();
                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
                downloadRecord = null;
                /*因为需要自己调用自己，所以这里提前手动关闭连接*/
                DownloadHelper.close(httpURLConnection);

                downloadByChildThread();
                return;
            }
            if (!TextUtils.isEmpty(eTag)) {
                downloadRecord.seteTag(eTag);
            } else if (!TextUtils.isEmpty(lastModified)) {
                downloadRecord.setLastModified(lastModified);
            } else {
                downloadRecord.seteTag("");
                downloadRecord.setLastModified("");
            }
            /*etag和lastmodified被修改之后，保存至本地*/
            saveDownloadCacheInfo(downloadRecord);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                /*不支持范围下载*/
                /*单线程下载*/
                downloadRecord.setSingleThreadDownload(contentLength, 1);
                /*如果本地存在之前下载一部分的文件，先删除*/
                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                DownloadHelper.deleteFile(getDownloadConfig().getSaveFile());
            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                /*支持范围下载*/
                /*如果文件小于30kb，就用单线程下载*/
                if (contentLength <= getDownloadConfig().getMinFileSize()) {
                    /*单线程下载*/
                    downloadRecord.setSingleThreadDownload(contentLength, 1);
                } else {
                    int threadNum = downloadConfig.getThreadNum();

                    if (downloadRecord.getFileSize() <= 0 || downloadRecord.getFileSize() != contentLength) {
                        /*如果用户手动删除了配置缓存文件，则重新下载*/
                        /*如果下载的文件大小和缓存的配置大小不一致，重新下载,删除需要下载的文件缓存*/
                        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
                        downloadRecord.setSingleThreadDownload(contentLength, threadNum);
                    }
                }
            } else {
                DownloadHelper.close(httpURLConnection);
                error();
                return;
            }
            DownloadHelper.close(httpURLConnection);
            /*开始准备下载*/
            prepareDownload();
        } catch (Exception e) {
            DownloadHelper.close(httpURLConnection);
            e.printStackTrace();
            error();
        }

    }

    /*开始准备下载*/
    private void prepareDownload() {
        List<DownloadRecord.FileRecord> fileRecordList = downloadRecord.getFileRecordList();

        int threadNum = downloadConfig.getThreadNum();
        setStatus(STATUS_PROGRESS);
        if (taskInfoList != null) {
            taskInfoList.clear();
        }
        for (int i = 0; i < threadNum; i++) {
            final DownloadRecord.FileRecord record = fileRecordList.get(i);
            long downloadLength = record.getDownloadLength();

            /*记录之前缓存的下载的进度*/
            localCacheSize += downloadLength;
            TaskInfo taskInfo = new TaskInfo(i, downloadConfig.getFileDownloadUrl(), record.getStartPoint(), downloadLength, record.getEndPoint(), downloadConfig.getTempSaveFile(), new TaskInfo.ReadStreamListener() {
                @Override
                public void readLength(long readLength) {
                    long currentProgress = record.getDownloadLength() + readLength;
                    record.setDownloadLength(currentProgress);
                    int status = getStatus();
                    if (status == STATUS_PAUSE || status == STATUS_ERROR || status == STATUS_DELETE) {
                        return;
                    }
                    progress(readLength);
                }

                @Override
                public void readComplete() {
                    /*每个taskinfo下载完之后检查其他的taskinfo是否也下载完成*/
                    checkOtherTaskInfoIsComplete();
                }

                @Override
                public void fail() {
                    /*如果有taskinfo出现错误，则通知其他taskinfo也改变状态为error*/
                    checkOtherTaskInfoIsError();
                }

                @Override
                public void needDelete() {
                    /*taskinfo告诉外部可以删除的时候，检查每个taskinfo是否都是可删除状态*/
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
        saveDownloadCacheInfo(downloadRecord);
        /*所有taskinfo下载完才是真的下载完*/
//        downloadConfig.getTempSaveFile().renameTo(downloadConfig.getSaveFile());
        /*现在完成之后进行文件合并*/
        boolean result = false;
        if (taskInfoList.size() == 0) {
            result = new File(downloadConfig.getTempSaveFile().getParent(), downloadConfig.getTempSaveFile().getName() + "0").renameTo(downloadConfig.getSaveFile());
        } else {
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(downloadConfig.getSaveFile());

                for (int i = 0; i < taskInfoList.size(); i++) {
                    /*每个task下载的临时文件*/
                    File tempFile = new File(downloadConfig.getTempSaveFile().getAbsolutePath() + i);

                    byte[] buff = new byte[2048 * 10];
                    int len = 0;
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tempFile));

                    while ((len = bis.read(buff)) != -1) {
                        outputStream.write(buff, 0, len);
                    }
                }
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                    }
                } catch (Exception e) {
                    result=false;
                }
            }
        }
        if (result) {
            success(downloadConfig.getSaveFile());
        } else {
            error();
        }
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
        /*所有taskinfo都可删除才是真的可以执行删除操作*/
        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
        DownloadHelper.deleteFile(downloadConfig.getSaveFile());
        DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
        delete();
    }

    /*如果下载任务中某个任务错误，则将其他任务改成error状态*/
    private void checkOtherTaskInfoIsError() {
        if (taskInfoList == null) {
            return;
        }
        for (TaskInfo info : taskInfoList) {
            info.changeStatus(STATUS_ERROR);
        }
        /*如果因为网络原因下载失败，但是app没有切前后台，则手动保存下载进度*/
        saveDownloadCacheInfo(downloadRecord);
//        下载时不清理已部分下载的缓存
//        error(true);
    }

    /*边下载边保存当前下载进度*/
    private void saveDownloadCacheInfo(DownloadRecord downloadRecord) {
        if (downloadRecord == null) {
            return;
        }
        DownloadHelper.get().saveRecord(downloadConfig.getDownloadSPName(), downloadRecord);
    }

    public String getFileDownloadUrl() {
        if (downloadConfig == null) {
            return "";
        }
        return downloadConfig.getFileDownloadUrl();
    }

    public DownloadConfig getDownloadConfig() {
        return downloadConfig;
    }

    public void notifySaveRecord() {
        saveDownloadCacheInfo(downloadRecord);
    }
}
