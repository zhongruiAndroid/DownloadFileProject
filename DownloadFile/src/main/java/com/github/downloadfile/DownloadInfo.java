package com.github.downloadfile;

import android.text.TextUtils;

import com.github.downloadfile.bean.DownloadRecord;
import com.github.downloadfile.helper.DownloadHelper;
import com.github.downloadfile.listener.FileDownloadListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    public DownloadInfo(DownloadConfig config, FileDownloadListener listener) {
        this.downloadConfig = config;
        this.downloadListener = listener;
        status = new AtomicInteger(0);
    }



    public void setDownloadListener(FileDownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    private void setStatus(int status) {
        if (FileDownloadManager.debug) {
            switch (status) {
                case STATUS_ERROR:
                    LG.i("下载状态:STATUS_ERROR");
                    break;
                case STATUS_SUCCESS:
                    LG.i("下载状态:STATUS_SUCCESS");
                    break;
                case STATUS_PAUSE:
                    LG.i("下载状态:STATUS_PAUSE");
                    break;
                case STATUS_DELETE:
                    LG.i("下载状态:STATUS_DELETE");
                    break;
                case STATUS_PROGRESS:
                    LG.i("下载状态:STATUS_PROGRESS");
                    break;
                case STATUS_CONNECT:
                    LG.i("下载状态:STATUS_CONNECT");
                    break;
                case STATUS_REQUEST:
                    LG.i("下载状态:STATUS_REQUEST");
                    break;
            }
        }
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

    public void deleteDownload(boolean deleteCacheFile) {
        if (deleteCacheFile) {
            if (FileDownloadManager.debug) {
                LG.i("删除文件");
            }
            if (downloadConfig != null) {
                DownloadHelper.get().getExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {
                        deleteTempFile(downloadConfig.getTempSaveFile());
                        DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
                    }
                });
            }
        }
        changeStatus(STATUS_DELETE);
    }
    public void release(){
        pauseDownload();
        downloadListener=null;
    }
    private void changeStatus(int changeStatus) {
        if (taskInfoList == null) {
            return;
        }
        if (getStatus() == STATUS_SUCCESS) {
            return;
        }
        for (TaskInfo info : taskInfoList) {
            /*删除状态由TaskInfo回调改变*/
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
        if (FileDownloadManager.debug) {
            LG.i("下载失败");
        }
        if (downloadConfig != null && !notClearCache) {
//            DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
            deleteTempFile(downloadConfig.getTempSaveFile());
            DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
        } else {
            saveDownloadCacheInfo(downloadRecord);
        }
        setStatus(STATUS_ERROR);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (downloadListener != null) {
                    downloadListener.onError();
                }
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
        notifySaveRecord();
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (downloadListener != null) {
                    downloadListener.onPause();
                }
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
                if (downloadListener != null) {
                    downloadListener.onDelete();
                }
            }
        });
    }

    private void success(final File file) {
        int status = getStatus();
        if (status == STATUS_SUCCESS) {
            return;
        }
        setStatus(STATUS_SUCCESS);
        if (FileDownloadManager.debug) {
            LG.i("下载完成:" + file.getAbsolutePath());
        }
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (downloadListener != null) {
                    downloadListener.onSuccess(file);
                }
            }
        });
    }

    private void connect(final long totalSize) {
        int status = getStatus();
        if (status == STATUS_CONNECT) {
            return;
        }
        this.totalSize = totalSize;
        setStatus(STATUS_CONNECT);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (downloadListener != null) {
                    downloadListener.onConnect(totalSize);
                }
            }
        });
    }

    private long preTime;
    private long tempDownloadSize;

    private void reset() {
        if (taskInfoList != null) {
            taskInfoList.clear();
        } else {
            taskInfoList = new ArrayList<>();
        }
        preTime = 0;
        tempDownloadSize = 0;
//        localCacheSize = 0;
    }

    private Runnable saveCacheRunnable = new Runnable() {
        @Override
        public void run() {
            if (getStatus() == STATUS_PROGRESS) {
                notifySaveRecord();
                DownloadHelper.get().getHandler().postDelayed(saveCacheRunnable, getDownloadConfig().getSaveFileTimeInterval());
            }
        }
    };

    private void progress() {
        if (getStatus() != STATUS_PROGRESS) {
            return;
        }
        if (taskInfoList == null || taskInfoList.isEmpty()) {
            return;
        }
        long progress = 0;
        for (TaskInfo info : taskInfoList) {
            if (info == null) {
                continue;
            }
            progress += info.getDownloadLength();
        }
        //计算网速
        if (downloadConfig.isNeedSpeed()) {
            long nowTime = System.currentTimeMillis();
            if (preTime <= 0) {
                tempDownloadSize = progress;
                preTime = nowTime;
            }
            long timeInterval = nowTime - preTime;
            if (timeInterval >= 1000) {
                final float speedBySecond = (progress - tempDownloadSize) * 1000f / timeInterval;
                tempDownloadSize = progress;
                preTime = nowTime;
                DownloadHelper.get().getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (downloadListener != null) {
                            downloadListener.onSpeed(Float.parseFloat(String.format("%.1f", speedBySecond)));
                        }
                    }
                });
            }
        }
        final long finalProgress = progress;
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (downloadListener != null) {
                    downloadListener.onProgress(finalProgress, totalSize);
                }
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
            if (downloadListener != null) {
                downloadListener.onError();
            }
        }
        String fileUrl = downloadConfig.getFileDownloadUrl();

        if (FileDownloadManager.debug) {
            LG.i("url下载地址:"+fileUrl);
        }
        if (TextUtils.isEmpty(fileUrl)) {
            if (downloadListener != null) {
                downloadListener.onError();
            }
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
        if (FileDownloadManager.debug) {
            LG.i("----------准备下载前的逻辑校验----------");
        }
        reset();
        /*下载完成后需要保存的文件*/
        File saveFile = downloadConfig.getSaveFile();
        if (!saveFile.getParentFile().exists()) {
            saveFile.getParentFile().mkdirs();
        }
        if (FileDownloadManager.debug) {
            LG.i("设置下载成功后的文件路径:" + saveFile.getAbsolutePath());
        }
        /*如果存在已下载完成的文件*/
        if (saveFile != null && saveFile.exists() && saveFile.isFile()) {
            if (FileDownloadManager.debug) {
                LG.i("文件已经存在:" + saveFile.getAbsolutePath());
            }
            if (downloadConfig.isIfExistAgainDownload()) {
                boolean deleteResult = DownloadHelper.deleteFile(saveFile);
                if (FileDownloadManager.debug) {
                    LG.i("已经存在的文件_删除" + (deleteResult ? "成功" : "失败"));
                }
//                downloadConfig.setSaveFile(DownloadHelper.reDownloadAndRename(saveFile, 1));
            } else {
                /*如果本地已存在下载的文件，直接返回*/
                long length = saveFile.length();
                if (FileDownloadManager.debug) {
                    LG.i("已存在的文件大小(字节):" + length);
                }
                connect(length);
                progress();
                success(saveFile);
                return;
            }
        }
        /*先判断内存是否存在数据，不存在再读取本地缓存配置,用于[下载-暂停-再下载]流程*/
        if (DownloadRecord.isEmpty(downloadRecord)) {
            downloadRecord = DownloadHelper.get().getRecord(downloadConfig.getDownloadSPName(), getDownloadConfig().getUnionId());
        }
        if (FileDownloadManager.debug) {
            if (DownloadRecord.isEmpty(downloadRecord)) {
                LG.i("未获取到下载记录");
            } else {
                LG.i("获取到已存在的下载记录");
            }
        }
        /*如果没有下载记录，那么需要删除之前已经下载的临时文件*/
        /*或者如果需要重新下载，忽略之前的下载进度*/
        if (DownloadRecord.isEmpty(downloadRecord) || (downloadConfig != null && downloadConfig.isReDownload())) {
//            DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
            deleteTempFile(getDownloadConfig().getTempSaveFile());
            DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
            downloadRecord = null;
            if (FileDownloadManager.debug) {
                LG.i("删除已存在的下载记录");
            }
        }
        /*如果本地有下载记录，但是下载一部分的本地文件已经不存在了*/
        if (downloadRecord != null && downloadRecord.hasDownloadRecord()) {
            File downloadTempFile = getDownloadTempFile(downloadConfig.getTempSaveFile());
            if (downloadTempFile != null && !downloadTempFile.exists()) {
                if (FileDownloadManager.debug) {
                    LG.i("存在下载记录,但不存在已下载的temp文件,删除已存在的下载记录");
                }
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
                downloadRecord = null;
            }
        }
        if (FileDownloadManager.debug) {
            LG.i("开始请求网络文件");
        }
        HttpURLConnection httpURLConnection = null;
        try {
            setStatus(STATUS_REQUEST);
            URL url = new URL(downloadConfig.getFileDownloadUrl());
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range", "bytes=" + 0 + "-");
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            String eTag = httpURLConnection.getHeaderField("ETag");
            String lastModified = httpURLConnection.getHeaderField("Last-Modified");
            long contentLength = getContentLength(httpURLConnection);
            if (FileDownloadManager.debug) {
                LG.i("请求响应码:" + responseCode);
                LG.i("获取到网络文件ETag:" + eTag);
                LG.i("获取到网络文件Last-Modified:" + lastModified);
                LG.i("获取到网络文件长度为(字节):" + contentLength);
            }
            if (contentLength < 0) {
                DownloadHelper.close(httpURLConnection);
                /*有可能状态码=200，但是内容长度为null*/
                error();
                return;
            }
            connect(contentLength);
            if (!DownloadHelper.hasFreeSpace(FileDownloadManager.getContext(), contentLength)) {
                DownloadHelper.close(httpURLConnection);
                if (FileDownloadManager.debug) {
                    LG.i("存储空间不足");
                }
                //储存空间不足
                error(true);
                return;
            }
            /*如果首次下载*/
            if (downloadRecord == null || downloadRecord.getFileSize() <= 0) {
                if (FileDownloadManager.debug) {
                    LG.i("需要重新下载");
                }
                downloadRecord = new DownloadRecord(contentLength, downloadConfig.getThreadNum());
                downloadRecord.setFileSize(contentLength);
                downloadRecord.setDownloadUrl(downloadConfig.getFileDownloadUrl());
                downloadRecord.setSaveFilePath(downloadConfig.getSaveFile().getAbsolutePath());
                downloadRecord.setUniqueId(downloadConfig.getUnionId());
            }
            /*上次请求的eTag和lastModified,如果和这次请求返回的不一样，则从头开始下载*/
            String preETag = downloadRecord.geteTag();
            String preLastModified = downloadRecord.getLastModified();
            if (FileDownloadManager.debug) {
                LG.i("获取文件上次下载记录的ETag:" + preETag);
                LG.i("获取文件上次下载记录的Last-Modified:" + preLastModified);
            }
            if (!TextUtils.isEmpty(eTag) && !TextUtils.isEmpty(preETag) && !TextUtils.equals(eTag, preETag)) {
                if (FileDownloadManager.debug) {
                    LG.i("网络文件和本地文件ETag不匹配,准备重新下载");
                }
                /*文件被修改*/
//                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                deleteTempFile(getDownloadConfig().getTempSaveFile());
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
                downloadRecord = null;

                /*因为需要自己调用自己，所以这里提前手动关闭连接*/
                DownloadHelper.close(httpURLConnection);

                downloadByChildThread();
                return;
            } else if (!TextUtils.isEmpty(lastModified) && !TextUtils.isEmpty(preLastModified) && !TextUtils.equals(lastModified, preLastModified)) {
                if (FileDownloadManager.debug) {
                    LG.i("网络文件和本地文件Last-Modified不匹配,准备重新下载");
                }
                /*文件被修改*/
//                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                deleteTempFile(getDownloadConfig().getTempSaveFile());
                DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
                downloadRecord = null;
                /*因为需要自己调用自己，所以这里提前手动关闭连接*/
                DownloadHelper.close(httpURLConnection);

                downloadByChildThread();
                return;
            }
            if (!TextUtils.isEmpty(eTag)) {
                if (FileDownloadManager.debug) {
                    LG.i("记录网络文件ETag");
                }
                downloadRecord.seteTag(eTag);
            } else if (!TextUtils.isEmpty(lastModified)) {
                if (FileDownloadManager.debug) {
                    LG.i("记录网络文件Last-Modified");
                }
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
//                DownloadHelper.deleteFile(getDownloadConfig().getTempSaveFile());
                deleteTempFile(getDownloadConfig().getTempSaveFile());
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
//                        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
                        deleteTempFile(downloadConfig.getTempSaveFile());
                        downloadRecord.setSingleThreadDownload(contentLength, threadNum);
                    }
                }
            } else {
                if (FileDownloadManager.debug) {
                    LG.i("请求异常,状态码:" + responseCode);
                }
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
            /*如果下载一部分，因为网络原因导致失败，然后再点击重试，无网络导致失败，此时不做删除缓存的动作*/
            error(true);
        }

    }

    /*开始准备下载*/
    private void prepareDownload() {
        int statusTemp = getStatus();
        if(statusTemp==STATUS_PAUSE||statusTemp==STATUS_DELETE){
            return;
        }
        List<DownloadRecord.FileRecord> fileRecordList = downloadRecord.getFileRecordList();

        int threadNum = fileRecordList.size();
        /*如果已有的下载记录和设置的下载线程数量不一样，以缓存记录为准*/
        downloadConfig.setThreadNum(threadNum);
        setStatus(STATUS_PROGRESS);
        if (taskInfoList != null) {
            taskInfoList.clear();
        }

        if (FileDownloadManager.debug) {
            if (threadNum <= 1) {
                LG.i("准备单线程下载");
            } else {
                LG.i("准备多线程下载,线程数量:" + threadNum);
            }
        }
        for (int i = 0; i < threadNum; i++) {
            final DownloadRecord.FileRecord record = fileRecordList.get(i);
            long downloadLength = record.getDownloadLength();

            /*记录之前缓存的下载的进度*/
//            localCacheSize += downloadLength;
            TaskInfo taskInfo = new TaskInfo(i, downloadConfig.getFileDownloadUrl(), record.getStartPoint(), downloadLength, record.getEndPoint(), downloadConfig.getTempSaveFile(), new TaskInfo.ReadStreamListener() {
                @Override
                public void readLength(long readLength) {
                    long currentProgress = record.getDownloadLength() + readLength;
                    /*记录下载长度，用于下载中时保存下载的记录*/
                    record.setDownloadLength(currentProgress);
                    int status = getStatus();
                    if (status == STATUS_PAUSE || status == STATUS_ERROR || status == STATUS_DELETE) {
                        return;
                    }
                    progress();
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
        /*没3秒执行一次缓存数据*/
        DownloadHelper.get().getHandler().postDelayed(saveCacheRunnable, getDownloadConfig().getSaveFileTimeInterval());
    }

    public static int bufferSize = 1024 * 1024 * 2;

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
        /*保存下载记录*/
        notifySaveRecord();
        if (FileDownloadManager.debug) {
            if (getDownloadConfig().getThreadNum() <= 1) {
                LG.i("下载结束,准备修改文件名字");
            } else {
                LG.i("下载结束,准备合并文件");
            }
        }
        /*所有taskinfo下载完才是真的下载完*/
//        downloadConfig.getTempSaveFile().renameTo(downloadConfig.getSaveFile());
        /*现在完成之后进行文件合并*/
        int taskInfoSize = taskInfoList.size();
        boolean result = false;
        if (taskInfoSize == 1) {
            result = new File(downloadConfig.getTempSaveFile().getParent(), downloadConfig.getTempSaveFile().getName() + "0").renameTo(downloadConfig.getSaveFile());
        } else {
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(downloadConfig.getSaveFile());

                FileInputStream fileInputStream = null;
                BufferedInputStream bis = null;
                File tempFile = null;
                for (int i = 0; i < taskInfoList.size(); i++) {
                    /*每个task下载的临时文件*/
                    tempFile = new File(downloadConfig.getTempSaveFile().getAbsolutePath() + i);

                    byte[] buff = new byte[bufferSize];
                    int len = 0;

                    fileInputStream = new FileInputStream(tempFile);
                    bis = new BufferedInputStream(fileInputStream);

                    while ((len = bis.read(buff)) != -1) {
                        outputStream.write(buff, 0, len);
                    }
                    outputStream.flush();

                    bis.close();
                    fileInputStream.close();
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
                    result = false;
                }
            }
        }
        if (result) {
            if (taskInfoSize > 1) {
                /*如果是多线程下载*/
                /*如果文件组合完成，则删除临时文件*/
                deleteTempFile(downloadConfig.getTempSaveFile());
            }
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
//        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
        deleteTempFile(downloadConfig.getTempSaveFile());
        DownloadHelper.deleteFile(downloadConfig.getSaveFile());
        DownloadHelper.get().clearRecordByUnionId(downloadConfig.getDownloadSPName(), downloadConfig.getUnionId());
        delete();
    }

    private File getDownloadTempFile(File tempFile) {
        if (tempFile == null) {
            return null;
        }
        return new File(tempFile.getParent(), tempFile.getName() + "0");
    }

    private void deleteTempFile(File tempFile) {
        if (tempFile == null) {
            return;
        }
        if (tempFile.exists()) {
            DownloadHelper.deleteFile(tempFile);
        }
        int position = 0;
        int count = 0;
        File file;
        while (count <= 2) {
            file = new File(tempFile.getParent(), tempFile.getName() + position);
            if (file.exists()) {
                DownloadHelper.deleteFile(file);
            } else {
                count += 1;
            }
            position += 1;
        }
    }

    /*如果下载任务中某个任务错误，则将其他任务改成error状态*/
    private void checkOtherTaskInfoIsError() {
        if (taskInfoList == null) {
            return;
        }
        for (TaskInfo info : taskInfoList) {
            info.changeStatus(STATUS_ERROR);
        }
//        下载时不清理已部分下载的缓存,但是需要更新下载状态
        /*如果因为网络原因下载失败，保存下载进度*/
        error(true);
    }

    /*边下载边保存当前下载进度*/
    private void saveDownloadCacheInfo(DownloadRecord downloadRecord) {
        if (downloadRecord == null) {
            return;
        }
        if (FileDownloadManager.debug) {
            LG.i("保存下载记录到本地");
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
