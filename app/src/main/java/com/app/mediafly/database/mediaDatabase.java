package com.app.mediafly.database;

import static androidx.constraintlayout.widget.StateSet.TAG;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class mediaDatabase extends SQLiteOpenHelper {
    private static final String dbname = "media.db";

    public mediaDatabase(@Nullable Context context) {
        super(context, dbname, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String q = "create table media ( size text ,format text ," +
                " fileName text primary key,startTime text ," +
                " endTime text, " +
                " sequence integer , event integer," +
                " isDownloaded integer,actionUrl text,startDate text,endDate text)";
        sqLiteDatabase.execSQL(q);

    }

    public Cursor getFileNameData() {
        SQLiteDatabase dbMedia = this.getWritableDatabase();
        Cursor cursor = dbMedia.rawQuery("select * from media ", null);
        return cursor;
    }


    public boolean checkDbIsEmpty() {
        SQLiteDatabase dbMedia = this.getWritableDatabase();

        Cursor mCursor = dbMedia.rawQuery("SELECT * FROM " + "media", null);
        Boolean rowExists;

        // DO SOMETHING WITH CURSOR
        // I AM EMPTY
        rowExists = !mCursor.moveToFirst();

        return rowExists;
    }

    public void clearDatabase() {
        SQLiteDatabase dbMedia = this.getWritableDatabase();
        String clearDBQuery = "DELETE FROM " + "media";
        dbMedia.execSQL(clearDBQuery);
    }

    public boolean insert_data(
            String size, String format, String fileName,
            String startTime, String endTime,
            Integer sequence, Integer event,
            Integer isDownloaded, String actionUrl, String startDate, String endDate) {

        SQLiteDatabase dbMedia = this.getWritableDatabase();
        ContentValues c = new ContentValues();
        c.put("size", size);
        c.put("format", format);
        c.put("fileName", fileName);
        c.put("startTime", startTime);
        c.put("endTime", endTime);
        c.put("sequence", sequence);
        c.put("event", event);
        c.put("isDownloaded", isDownloaded);
        c.put("actionUrl", actionUrl);
        c.put("startDate", startDate);
        c.put("endDate", endDate);

        long r = dbMedia.insert("media", null, c);
        return r != -1;

    }

    public boolean updateData(String fileName) {

        SQLiteDatabase dbMedia = this.getWritableDatabase();
        ContentValues c = new ContentValues();
        c.put("isDownloaded", 1);

        Cursor cursor = dbMedia.rawQuery("select * from media where fileName=?", new String[]{fileName});
        if (cursor.getCount() > 0) {
            long r = dbMedia.update("media", c, "fileName=?", new String[]{fileName});
            return r != -1;
        } else return false;
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("drop table if exists media");
        onCreate(sqLiteDatabase);
    }


/*
    public ArrayList<String> getList(String type) {
        ArrayList<String> typeList = new ArrayList<>();

        // SELECT * FROM POSTS
        // LEFT OUTER JOIN USERS
        // ON POSTS.KEY_POST_USER_ID_FK = USERS.KEY_USER_ID
        String POSTS_SELECT_QUERY =
                String.format("SELECT * FROM media",
                        type);

        // "getReadableDatabase()" and "getWriteableDatabase()" return the same object (except under low
        // disk space scenarios)
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(POSTS_SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    typeList.add(cursor.getString(cursor.getColumnIndex(type)));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get posts from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return typeList;
    }
*/

    public ArrayList<String> getDownloadedFileList(String type) {
        ArrayList<String> downloadedList = new ArrayList<>();

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery("select * from media where isDownloaded=?", new String[]{"1"});
        try {
            if (cursor.moveToFirst()) {
                do {
                    downloadedList.add(cursor.getString(cursor.getColumnIndex(type)));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get downloaded list from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return downloadedList;
    }

    public ArrayList<String> getPendingFileNames() {
        ArrayList<String> pendingList = new ArrayList<>();

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery("select * from media where isDownloaded=?", new String[]{"0"});
        try {
            if (cursor.moveToFirst()) {
                do {
                    pendingList.add(cursor.getString(cursor.getColumnIndex("fileName")));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to get pending list from database");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return pendingList;

    }

    public void deleteARow(String fileName) {
        SQLiteDatabase database = this.getWritableDatabase();
        long result = database.delete("media", "fileName=?", new String[]{fileName});
        if (result == -1) {
            Log.d(TAG, "Failed to delete");
        } else Log.d(TAG, "Successfully deleted");
    }


}
