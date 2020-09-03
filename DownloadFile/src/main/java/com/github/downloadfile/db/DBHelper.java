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

    private static final int VERSION =1;
    private SQLiteDatabase database;
    private AtomicInteger openCount;
    private DBHelper(@Nullable Context context) {
        super(context, DBDao.dbName, null, VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBDao.sqlCreateTable);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DBDao.sqlDropTable);
        db.execSQL(DBDao.sqlCreateTable);
    }
    
}
