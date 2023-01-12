package com.github.downloadfile.bean;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DownloadRecord {
    private long fileSize;
    private List<FileRecord> fileRecordList;
    private String downloadUrl;
    /*保存路径*/
    private String saveFilePath;
    /*下载任务的唯一标识*/
    private String uniqueId;
    private String lastModified;
    private String eTag;

    /*从缓存获取数据*/
    private DownloadRecord(long fileSize, String uniqueId) {
        this.fileSize = fileSize;
        fileRecordList = new ArrayList<>();
        this.uniqueId = uniqueId;
    }

    /*第一次初始化下载*/
    public DownloadRecord(long fileSize, int threadNum) {
        this.fileSize = fileSize;
        setSingleThreadDownload(fileSize, threadNum);
    }

    public void setSingleThreadDownload(long fileSize, int threadNum) {
        if(threadNum<=0){
            threadNum=1;
        }
        this.fileSize = fileSize;
        fileRecordList = new ArrayList<>();
        long average = fileSize / threadNum;
        for (int i = 0; i < threadNum; i++) {
            long start = average * i;
            long end;
            if (i == (threadNum - 1)) {
                end = fileSize-1;
            } else {
                end = start + average - 1;
            }
            DownloadRecord.FileRecord record = new DownloadRecord.FileRecord();
            record.setStartPoint(start);
            record.setEndPoint(end);
            addFileRecordList(record);
        }
    }


    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
        if (TextUtils.isEmpty(uniqueId) && !TextUtils.isEmpty(getDownloadUrl())) {
            this.uniqueId = getDownloadUrl().hashCode() + "";
        }
    }

    public String getUniqueId() {
        if (TextUtils.isEmpty(uniqueId)) {
            uniqueId = "";
            if (!TextUtils.isEmpty(getDownloadUrl())) {
                this.uniqueId = getDownloadUrl().hashCode() + "";
            }
        }
        return uniqueId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        if (TextUtils.isEmpty(uniqueId) && !TextUtils.isEmpty(downloadUrl)) {
            uniqueId = downloadUrl.hashCode() + "";
        }
    }

    public String getSaveFilePath() {
        return saveFilePath;
    }

    public void setSaveFilePath(String saveFilePath) {
        this.saveFilePath = saveFilePath;
    }

    public boolean isCompleteDownload() {
        List<FileRecord> fileRecordList = getFileRecordList();
        if (fileRecordList.isEmpty()) {
            return false;
        }
        long fileDownloadLength = 0;
        for (FileRecord item : fileRecordList) {
            fileDownloadLength = fileDownloadLength + item.getDownloadLength();
        }
        return this.fileSize == fileDownloadLength && this.fileSize > 0;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public List<FileRecord> getFileRecordList() {
        if (fileRecordList == null) {
            fileRecordList = new ArrayList<>();
        }
        return fileRecordList;
    }

    public void addFileRecordList(FileRecord record) {
        getFileRecordList().add(record);
    }


    /*如果有记录下载记录*/
    public boolean hasDownloadRecord() {
        List<FileRecord> fileRecordList = getFileRecordList();
        for (FileRecord record : fileRecordList) {
            if (record == null || record.getDownloadLength() <= 0) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static class FileRecord {
        /*片段起始点*/
        private long startPoint;
        /*片段截止点*/
        private long endPoint;
        /*该片段下载的长度*/
        private long downloadLength;

        public long getStartPoint() {
            return startPoint;
        }

        public void setStartPoint(long startPoint) {
            this.startPoint = startPoint;
        }

        public long getEndPoint() {
            return endPoint;
        }

        public void setEndPoint(long endPoint) {
            this.endPoint = endPoint;
        }

        public long getDownloadLength() {
            return downloadLength;
        }

        public void setDownloadLength(long downloadLength) {
            this.downloadLength = downloadLength;
        }
    }

    public static DownloadRecord fromJson(String json) {
        DownloadRecord downloadRecord;
        if (TextUtils.isEmpty(json)) {
            downloadRecord = new DownloadRecord(0, 1);
            return downloadRecord;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            long fileSize = jsonObject.optLong("fileSize");
            String downloadUrl = jsonObject.optString("downloadUrl");
            String lastModified = jsonObject.optString("lastModified");
            String eTag = jsonObject.optString("eTag");
            String saveFilePath = jsonObject.optString("saveFilePath");
            String uniqueId = jsonObject.optString("uniqueId");

            JSONArray fileRecordList = jsonObject.optJSONArray("fileRecordList");
            downloadRecord = new DownloadRecord(fileSize, uniqueId);
            downloadRecord.fileSize = fileSize;
            downloadRecord.downloadUrl = downloadUrl;
            downloadRecord.lastModified = lastModified;
            downloadRecord.eTag = eTag;
            downloadRecord.saveFilePath = saveFilePath;
            downloadRecord.uniqueId = uniqueId;

            if (fileRecordList != null && fileRecordList.length() > 0) {
                for (int i = 0; i < fileRecordList.length(); i++) {
                    JSONObject itemObj = fileRecordList.getJSONObject(i);
                    FileRecord record = new FileRecord();
                    record.setStartPoint(itemObj.optLong("startPoint"));
                    record.setEndPoint(itemObj.optLong("endPoint"));
                    /*因为断点下载的起始位置减一，相应的已经下载的长度也要减一*/
                    long downloadLength = itemObj.optLong("downloadLength");
                    if (downloadLength > 0) {
                        downloadLength = downloadLength - 1;
                    }
                    record.setDownloadLength(downloadLength);
                    downloadRecord.addFileRecordList(record);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            downloadRecord = new DownloadRecord(0, "");
        }
        return downloadRecord;
    }

    public static boolean isEmpty(DownloadRecord downloadRecord) {
        return downloadRecord == null || downloadRecord.getFileSize() <= 0;
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fileSize", getFileSize());
            jsonObject.put("downloadUrl", getDownloadUrl());
            jsonObject.put("uniqueId", getUniqueId());
            jsonObject.put("lastModified", getLastModified());
            jsonObject.put("eTag", geteTag());
            jsonObject.put("saveFilePath", getSaveFilePath());
            JSONArray jsonArray = new JSONArray();
            for (FileRecord fileRecord : getFileRecordList()) {
                JSONObject itemJson = new JSONObject();
                itemJson.put("startPoint", fileRecord.getStartPoint());
                itemJson.put("endPoint", fileRecord.getEndPoint());
                itemJson.put("downloadLength", fileRecord.getDownloadLength());
                jsonArray.put(itemJson);
            }
            jsonObject.put("fileRecordList", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
