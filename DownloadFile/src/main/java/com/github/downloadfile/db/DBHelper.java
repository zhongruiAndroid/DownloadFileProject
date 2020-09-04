package com.github.downloadfile.db;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.downloadfile.DownloadManager;

import java.util.concurrent.atomic.AtomicInteger;

public class DBHelper extends SQLiteOpenHelper {
    /**********************************************************/
    private static DBHelper singleObj;

    public static DBHelper get(){
        if(singleObj==null){
            synchronized (DBHelper.class){
                if(singleObj==null){
                    singleObj=new DBHelper(DownloadManager.getContext());
                }
            }
        }
        return singleObj;
    }
    /**********************************************************/

    public static final String dbName = "db_zr_multi_download";
    public static final String tableName = "table_download";
    public static final String _id = "_id";
    public static final String unique_id = "unique_id";
    public static final String file_size = "file_size";
    public static final String download_url = "download_url";
    public static final String download_path = "download_path";
    public static final String download_info = "download_info";
    public static final String sqlCreateTable = "create table " + tableName + "(" +
            _id + " integer primary key autoincrement not null," +
            unique_id + " text ," +
            file_size + " text ," +
            download_url + " text ," +
            download_info + " text " +
            ")";

    public static final String sqlDropTable =  "drop table " + tableName ;
    /**********************************************************/

    private static final int VERSION =1;
    private SQLiteDatabase database;
    private AtomicInteger openCount;
    private DBHelper(@Nullable Context context) {
        super(context, dbName, null, VERSION);
        openCount=new AtomicInteger(0);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sqlCreateTable);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(sqlDropTable);
        db.execSQL(sqlCreateTable);
    }


    public SQLiteDatabase openDatabase(){
        if(openCount.incrementAndGet()==1){
            database = getWritableDatabase();
        }
        return database;
    }
    public void closeDatabase(){
        if(openCount.decrementAndGet()==0){
            if(database!=null&&database.isOpen()){
                database.close();
            }
        }
    }
    public void forceCloseDatabase(){
        if (openCount != null) {
            openCount.set(0);
        }
        if(database!=null&&database.isOpen()){
            database.close();
        }
    }
}
