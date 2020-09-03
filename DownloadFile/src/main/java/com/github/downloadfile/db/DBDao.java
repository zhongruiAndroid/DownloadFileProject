package com.github.downloadfile.db;

public class DBDao {
    /**********************************************************/
    private static DBDao singleObj;

    private DBDao() {
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

    public static final String dbName = "db_zr_multi_download";
    public static final String tableName = "table_download";
    public static final String _id = "_id";
    public static final String unique_id = "unique_id";
    public static final String file_size = "file_size";
    public static final String url = "url";
    public static final String download_info = "download_info";
    public static final String sqlCreateTable = "create table " + tableName + "(" +
            _id + "integer primary key autoincrement not null," +
            unique_id + " text ," +
            file_size + "text ," +
            url + "text ," +
            download_info + "text ," +
            ")";

    public static final String sqlDropTable =  "drop table " + tableName ;
}
