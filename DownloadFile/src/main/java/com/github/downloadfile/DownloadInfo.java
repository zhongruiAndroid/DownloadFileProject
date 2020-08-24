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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadInfo {
    private int fileSize;
    private DownloadListener downloadListener;
    private DownloadConfig downloadConfig;
    private volatile DownloadRecord downloadRecord;
    private AtomicInteger multiCompleteNum;
    private AtomicInteger multiPauseNum;
    private AtomicInteger multiDeleteNum;



    public static final int status_error=1;
    public static final int status_success=2;
    public static final int status_pause=3;
    public static final int status_delete=4;

    private AtomicInteger status;

    public DownloadInfo(DownloadConfig config, DownloadListener listener) {
        this.downloadConfig = config;
        this.downloadListener = listener;
        status = new AtomicInteger(0);
        multiCompleteNum = new AtomicInteger(0);
        multiPauseNum = new AtomicInteger(0);
        multiDeleteNum = new AtomicInteger(0);
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
                public void onDelete() {

                }

                @Override
                public void onError() {

                }
            };
        }
        return downloadListener;
    }
    public void pauseDownload(){
        int status=this.status.get();
        if(status==status_error||status==status_success||status==status_pause||status==status_delete){
            return;
        }
        this.status.set(status_pause);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onPause();
            }
        });
    }
    private void error() {
        int status=this.status.get();
        if(status==status_error){
            return;
        }
        this.status.set(status_error);
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onError();
            }
        });
    }

    private void pause() {
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onPause();
            }
        });
    }
    private void delete() {
        DownloadHelper.get().getHandler().post(new Runnable() {
            @Override
            public void run() {
                getDownloadListener().onDelete();
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
        downloadConfig.setFileDownloadUrl(fileUrl);
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
                    Log.i("====", "=========contentLength:" + contentLength);
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


                    int threadNum = downloadConfig.getThreadNum();


                    /*读取本地缓存配置*/
                    downloadRecord = DownloadHelper.get().getRecord(fileUrl.hashCode() + "");
                    Log.i("=====", "=====toJson=" + downloadRecord.toJson());
                    if (false) {
                        return;
                    }
                    if (downloadRecord == null || downloadRecord.getFileSize() <= 0) {
                        //如果用户手动删除了配置缓存文件，则重新下载
                        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());
                        downloadRecord = new DownloadRecord(contentLength, fileUrl);
                        downloadRecord.setThreadNum(threadNum);


                    } else if (downloadRecord != null && downloadRecord.getFileSize() != contentLength) {
                        /*如果下载的文件大小和缓存的配置大小不一致，重新下载*/

                        /*删除配置缓存*/
//                        DownloadHelper.deleteFile(downloadConfig.getCacheRecordFile());
                        /*删除需要下载的文件缓存*/
                        DownloadHelper.deleteFile(downloadConfig.getTempSaveFile());

                        downloadRecord = new DownloadRecord(contentLength, fileUrl);
                        downloadRecord.setThreadNum(threadNum);
                    }

                    /*多线程下载*/
                    canMultiDownload( );
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
            DownloadHelper.get().clearRecord(downloadConfig.getFileDownloadUrl().hashCode() + "");
        }
        int threadNum = downloadConfig.getThreadNum();
        final List<DownloadRecord.FileRecord> fileRecordList = downloadRecord.getFileRecordList();
        for (int i = 0; i < threadNum; i++) {

            final DownloadRecord.FileRecord record = fileRecordList.get(i);
            long downloadLength = record.getDownloadLength();
            if (downloadLength > 0) {
                downloadLength -= 1;
            }
            TaskInfo taskInfo=new TaskInfo(downloadConfig.getFileDownloadUrl(), record.getStartPoint() + downloadLength, record.getEndPoint(), downloadConfig.getTempSaveFile(), new TaskInfo.ReadStreamListener() {
                @Override
                public void readLength(long readLength) {
                    record.setDownloadLength(record.getDownloadLength() + readLength);
                    saveDownloadCacheInfo(downloadRecord);
                }
                @Override
                public void readComplete() {
                    int num = multiCompleteNum.incrementAndGet();
                    if (num == downloadConfig.getThreadNum()) {
                        downloadConfig.getTempSaveFile().renameTo(downloadConfig.getSaveFile());
                        DownloadHelper.get().clearRecord(downloadRecord.getUniqueId());
                        status.set(status_success);
                        success(downloadConfig.getSaveFile());
                    }
                }
                @Override
                public boolean notNeedRead() {
                    int status=DownloadInfo.this.status.get();
                    /*如果其他下载任务出现异常*/
                    if(status==status_error){
                        return true;
                    }
                    /*如果外部调用暂停方法*/
                    if (status == status_pause) {
                        int num = multiPauseNum.incrementAndGet();
                        if (num == downloadConfig.getThreadNum()) {
                            pause();
                        }
                        return true;
                    }
                    /*如果外部调用删除方法*/
                    if (status == status_delete) {
                        int num = multiDeleteNum.incrementAndGet();
                        if (num == downloadConfig.getThreadNum()) {
                            delete();
                        }
                        return true;
                    }
                    return false;
                }
                @Override
                public void fail() {
                    error();
                }
            });
            DownloadHelper.get().getExecutorService().execute(taskInfo);
        }
    }


    private void startMultiDownload(DownloadRecord.FileRecord record, long startPoint, long endPoint) {
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        // 随机访问文件，可以指定断点续传的起始位置
        BufferedInputStream bis = null;
        RandomAccessFile randomAccessFile = null;
        if (record == null) {
            Log.i("====", "=========record==null");
            error();
            return;
        }
        try {
            URL url = new URL(downloadConfig.getFileDownloadUrl());
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestProperty("Range", "bytes=" + startPoint + "-" + endPoint);
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == 416) {
                Log.i("====", "=====416");
            }

            if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                inputStream = httpURLConnection.getInputStream();

                File targetFile = downloadConfig.getTempSaveFile();
                byte[] buff = new byte[2048 * 15];
                int len = 0;
                bis = new BufferedInputStream(inputStream);
                randomAccessFile = new RandomAccessFile(targetFile, "rwd");
                randomAccessFile.seek(startPoint);
                long downloadLength;
                while ((len = bis.read(buff)) != -1) {
                    randomAccessFile.write(buff, 0, len);
                    downloadLength = record.getDownloadLength();
                    record.setDownloadLength(downloadLength + len);
                    saveDownloadCacheInfo(downloadRecord);
//                    Log.i("=====",index+"====="+new Gson().toJson(downloadRecord));

                    /*如果其他下载任务出现异常*/
                    if(status==status_error){
                        return;
                    }
                    /*如果外部调用暂停方法*/
                    if (status == status_pause) {
                        int num = multiPauseNum.incrementAndGet();
                        if (num == downloadConfig.getThreadNum()) {
                            pause();
                        }
                        return;
                    }
                    /*如果外部调用删除方法*/
                    if (status == status_delete) {
                        int num = multiDeleteNum.incrementAndGet();
                        if (num == downloadConfig.getThreadNum()) {
                            delete();
                        }
                        return;
                    }

                }
                int num = multiCompleteNum.incrementAndGet();
                if (num == downloadConfig.getThreadNum()) {
                    long endTime = System.currentTimeMillis();
                    downloadConfig.getTempSaveFile().renameTo(downloadConfig.getSaveFile());
                    DownloadHelper.get().clearRecord(downloadRecord.getUniqueId());
                    status=status_success;
                    success(downloadConfig.getSaveFile());
                }
            } else {
                error();
            }
        } catch (Exception e) {
            e.printStackTrace();
            error();
        } finally {
            DownloadHelper.close(randomAccessFile);
            DownloadHelper.close(bis);
            DownloadHelper.close(inputStream);
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    private AtomicLong saveDownloadInfoTime = new AtomicLong(0);

    /*边下载边保存当前下载进度*/
    public void saveDownloadCacheInfo(DownloadRecord downloadRecord) {
        if (downloadRecord == null) {
            return;
        }
        long nowTime = System.currentTimeMillis();
        if (saveDownloadInfoTime == null) {
            saveDownloadInfoTime = new AtomicLong(0);
        }
        if (nowTime - saveDownloadInfoTime.get() < 1500) {
            return;
        }
        saveDownloadInfoTime.set(nowTime);
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
        downloadRecord = DownloadHelper.get().getRecord(downloadConfig.getFileDownloadUrl().hashCode() + "");
        if (downloadRecord == null || downloadRecord.getFileSize() <= 0) {
            /*重新下载*/
            File tempSaveFile = downloadConfig.getTempSaveFile();
            if (tempSaveFile != null) {
                tempSaveFile.delete();
            }
            downloadRecord = new DownloadRecord(1, downloadConfig.getFileDownloadUrl());
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
