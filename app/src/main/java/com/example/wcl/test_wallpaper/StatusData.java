package com.example.wcl.test_wallpaper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by wangchenlong on 15-1-16.
 */
public class StatusData {

    private static final String TAG = "DEBUG-WCL: " +
            StatusData.class.getSimpleName();

    static final int VERSION = 1;
    static final String DATABASE = "timeline.db";
    static final String TABLE = "timeline";

    public static final String C_ID = "_id";
    public static final String C_CREATED_AT = "created_at";
    public static final String C_TEXT = "txt";
    public static final String C_USER = "user";

    private static final String GET_ALL_ORDER_BY = C_CREATED_AT + " DESC";
    private static final String[] MAX_CREATED_AT_COLUMNS =
            {"max(" + C_CREATED_AT + ")"};

    private static final String[] DB_TEXT_COLUMNS = {C_TEXT};

    // 数据库
    class DbHelper extends SQLiteOpenHelper {
        public DbHelper(Context context) {
            super(context, DATABASE, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "Creating database: " + DATABASE);
            db.execSQL("create table " + TABLE + " (" + C_ID + " int primary key, "
                    + C_CREATED_AT + " int, " + C_USER + " text, " + C_TEXT + " text)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("drop table " + TABLE);
            this.onCreate(db);
        }
    }

    final DbHelper mDbHelper;

    public StatusData(Context context) {
        mDbHelper = new DbHelper(context);
        Log.i(TAG, "Initialized data");
    }

    public void close() {
        mDbHelper.close();
    }

    public void insertOrIgnore(ContentValues values) {
        Log.d(TAG, "insertOrIgnore on " + values);
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        database.insertWithOnConflict(TABLE, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
        database.close();
    }

    public Cursor getStatusUpdates() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        return db.query(TABLE, null, null, null, null, null, GET_ALL_ORDER_BY);
    }

    public long getLastestStatusCreatedAtTime() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE, MAX_CREATED_AT_COLUMNS, null, null, null, null, null);
        return cursor.moveToNext() ? cursor.getLong(0) : Long.MIN_VALUE;
    }

    public String getStatusTextById(long id) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE, DB_TEXT_COLUMNS, C_ID + "=" + id, null, null, null, null);
        return cursor.moveToNext() ? cursor.getString(0) : null;
    }
}
