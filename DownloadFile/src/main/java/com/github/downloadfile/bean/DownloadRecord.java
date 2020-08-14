package com.github.downloadfile.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DownloadRecord implements Serializable {
    private int fileSize;
    private  List<FileRecord> fileRecordList;

    public DownloadRecord(int fileSize,int threadNum) {
        this.fileSize = fileSize;
        fileRecordList=new ArrayList<>();

        int average= fileSize / threadNum;
        for (int i = 0; i <threadNum; i++) {
            int start=average*i;
            int end;
            if(i==(threadNum-1)){
                end=fileSize;
            }else{
                end=start+average-1;
            }
            DownloadRecord.FileRecord record = new DownloadRecord.FileRecord();
            record.setStartPoint(start);
            record.setEndPoint(end);
            addFileRecordList(record);
        }
    }

    public int getFileSize() {
        return fileSize;
    }

    public synchronized List<FileRecord> getFileRecordList() {
        if(fileRecordList==null){
            fileRecordList=new ArrayList<>();
        }
        return fileRecordList;
    }

    public void addFileRecordList( FileRecord record) {
        getFileRecordList().add(record);
    }

    public static class FileRecord  implements Serializable {
        /*片段起始点*/
        private int startPoint;
        /*片段截止点*/
        private int endPoint;
        /*该片段下载的长度*/
        private  int downloadLength;

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
    public static void saveData(){

    }
    public static void getData(){

    }
}
