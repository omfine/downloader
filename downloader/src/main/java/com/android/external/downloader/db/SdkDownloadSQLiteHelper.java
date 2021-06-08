package com.android.external.downloader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 类功能描述：</br>
 */
public class SdkDownloadSQLiteHelper extends SQLiteOpenHelper {

    private static final String mDatabasename = "filedownloader";
    private static CursorFactory mFactory = null;
    private static final int mVersion = 1;

    public static final String TABLE_NAME = "sdkDownloadInfo"; //文件下载信息数据表名称

    public SdkDownloadSQLiteHelper(Context context) {
        super(context, mDatabasename, mFactory, mVersion);
    }

    public SdkDownloadSQLiteHelper(Context context, String name, CursorFactory factory,
                                   int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //创建文件下载信息数据表
        String downloadsql = "CREATE TABLE IF NOT EXISTS "+ TABLE_NAME +" ("
                + "id INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL , "
                + "userID VARCHAR, "
                + "taskID VARCHAR, "
                + "url VARCHAR, "
                + "filePath VARCHAR, "
                + "fileName VARCHAR, "
                + "fileSize VARCHAR, "
                + "downLoadSize VARCHAR "
                + ")";
        db.execSQL(downloadsql);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }
}
