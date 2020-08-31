package com.github.downloadfile.bean;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DownloadRecord implements Serializable {
    private long fileSize;
    private List<FileRecord> fileRecordList;
    private String uniqueId;

    /*从缓存获取数据*/
    private DownloadRecord(long fileSize, String uniqueId) {
        this.fileSize = fileSize;
        fileRecordList = new ArrayList<>();
        this.uniqueId = uniqueId;
    }
    /*第一次初始化下载*/
    public DownloadRecord(long fileSize, int threadNum) {
        this.fileSize = fileSize;
        fileRecordList = new ArrayList<>();
        long average = fileSize / threadNum;
        for (int i = 0; i < threadNum; i++) {
            long start = average * i;
            long end;
            if (i == (threadNum - 1)) {
                end = fileSize;
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

    public String getUniqueId() {
        if (TextUtils.isEmpty(uniqueId)) {
            uniqueId = "";
        }
        return uniqueId;
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

    public static class FileRecord implements Serializable {
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

        public static void test() {
        }

    }

    public static DownloadRecord fromJson(String json) {
        DownloadRecord downloadRecord;
        if (TextUtils.isEmpty(json)) {
            downloadRecord = new DownloadRecord(0,1);
            return downloadRecord;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            long fileSize = jsonObject.optLong("fileSize");
            String uniqueId = jsonObject.optString("uniqueId");
            JSONArray fileRecordList = jsonObject.optJSONArray("fileRecordList");
            downloadRecord = new DownloadRecord(fileSize, uniqueId);
            downloadRecord.fileSize = fileSize;
            if (fileRecordList != null && fileRecordList.length() > 0) {
                for (int i = 0; i < fileRecordList.length(); i++) {
                    JSONObject itemObj = fileRecordList.getJSONObject(i);
                    FileRecord record = new FileRecord();
                    record.setStartPoint(itemObj.optLong("startPoint"));
                    record.setEndPoint(itemObj.optLong("endPoint"));
                    record.setDownloadLength(itemObj.optLong("downloadLength"));
                    downloadRecord.addFileRecordList(record);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            downloadRecord = new DownloadRecord(0, "");
        }
        return downloadRecord;
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fileSize", getFileSize());
            jsonObject.put("uniqueId", getUniqueId());
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
