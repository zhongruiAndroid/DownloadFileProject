package com.github.downloadfile.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DownloadRecord implements Serializable {
    private List<FileRecord> fileRecordList;

    public List<FileRecord> getFileRecordList() {
        if(fileRecordList==null){
            fileRecordList=new ArrayList<>();
        }
        return fileRecordList;
    }

    public void addFileRecordList( FileRecord record) {
        getFileRecordList().add(record);
    }
    public void setFileRecordList(List<FileRecord> fileRecordList) {
        this.fileRecordList = fileRecordList;
    }
    public FileRecord getSingleDownloadRecord(){
        if(getFileRecordList().isEmpty()){
            FileRecord record = new FileRecord();
            getFileRecordList().add(record);
        }
        return getFileRecordList().get(0);
    }
    public static class FileRecord  implements Serializable {
        /*片段起始点*/
        private int startPoint;
        /*片段截止点*/
        private int endPoint;
        /*该片段下载的长度*/
        private int downloadLength;

        public int getStartPoint() {
            return startPoint;
        }

        public void setStartPoint(int startPoint) {
            this.startPoint = startPoint;
        }

        public int getEndPoint() {
            return endPoint;
        }

        public void setEndPoint(int endPoint) {
            this.endPoint = endPoint;
        }

        public int getDownloadLength() {
            return downloadLength;
        }

        public void setDownloadLength(int downloadLength) {
            this.downloadLength = downloadLength;
        }
        public static void test(){
        }

    }
}
