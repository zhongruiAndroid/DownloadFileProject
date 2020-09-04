package com.github.downloadfile.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.github.downloadfile.bean.DownloadRecord;

import static com.github.downloadfile.db.DBHelper.*;

public class DBDao {
    private DBHelper db;

    /**********************************************************/
    private static DBDao singleObj;
    private DBDao() {
        db=DBHelper.get();
    }
    public static DBDao get() {
        if (singleObj == null) {
            synchronized (DBDao.class) {
                if (singleObj == null) {
                    singleObj = new DBDao();
                }
            }
        }
        return singleObj;
    }

    /**********************************************************/



    public DownloadRecord getDownloadProgress(String uniqueId){
        DownloadRecord downloadRecord=new DownloadRecord(0,1);
        if(TextUtils.isEmpty(uniqueId)){
            return downloadRecord;
        }
        SQLiteDatabase sqLiteDatabase = db.openDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("select * from " + tableName + " where " + unique_id + " = ? ", new String[]{uniqueId});
        while(cursor.moveToNext()){
//            String unique_id = cursor.getString(cursor.getColumnIndex(DBHelper.unique_id));
            long file_size = cursor.getLong(cursor.getColumnIndex(DBHelper.file_size));
            String download_url = cursor.getString(cursor.getColumnIndex(DBHelper.download_url));
            String download_info = cursor.getString(cursor.getColumnIndex(DBHelper.download_info));

            downloadRecord.setUniqueId(uniqueId);
            downloadRecord.setFileSize(file_size);
            downloadRecord.setDownloadUrl(download_url);
            downloadRecord.getFileRecordList().clear();
            downloadRecord.getFileRecordList().addAll(DownloadRecord.fromJsonArray(download_info));
        }
        cursor.close();
        db.closeDatabase();
        return downloadRecord;
    }

    public synchronized void addOrUpdateDownloadProgress(DownloadRecord record){
        if(record==null){
            return;
        }
        SQLiteDatabase sqLiteDatabase = db.openDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("select count(0) from " + tableName + " where " + unique_id + " = ? ", new String[]{record.getUniqueId()});
        int count = cursor.getCount();
        if(count==0){
            addDownloadProgress(record);
        }else{
            updateDownloadProgress(record);
        }
        cursor.close();
        db.closeDatabase();
    }

    private void addDownloadProgress(DownloadRecord record) {
        SQLiteDatabase sqLiteDatabase = db.openDatabase();
        ContentValues contentValues=new ContentValues();
        contentValues.put(unique_id,record.getUniqueId());
        contentValues.put(file_size,record.getFileSize());
//        contentValues.put(download_path,record.getDownloadPath());
        contentValues.put(download_url,record.getDownloadUrl());
        contentValues.put(download_info,record.toJsonByRecordInfo());
        long insert = sqLiteDatabase.insert(tableName, null, contentValues);
        db.closeDatabase();
    }

    private void updateDownloadProgress(DownloadRecord record) {
        SQLiteDatabase sqLiteDatabase = db.openDatabase();
        ContentValues contentValues=new ContentValues();
        contentValues.put(file_size,record.getFileSize());
//        contentValues.put(download_path,record.getDownloadPath());
        contentValues.put(download_url,record.getDownloadUrl());
        contentValues.put(download_info,record.toJsonByRecordInfo());
        int update = sqLiteDatabase.update(tableName, contentValues, unique_id + " = ? ", new String[]{record.getUniqueId()});
        db.closeDatabase();
    }

    public void deleteDownloadProgress(String uniqueId){
        SQLiteDatabase sqLiteDatabase = db.openDatabase();
        int delete = sqLiteDatabase.delete(tableName, unique_id + " = ? ", new String[]{uniqueId});
        db.closeDatabase();
    }

}
