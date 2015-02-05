package com.vfdev.gettingthingsdonemusicapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import timber.log.Timber;


/** Singleton */
public class AppDBHandler extends SQLiteOpenHelper {

    public static final String DB_NAME="UserDB";
    public static final int DB_VERSION=1;

    public static final String DB_TABLE_SETTINGS="Settings";
    public static final String TABLE_SETTINGS_ID="Id";
    public static final String TABLE_SETTINGS_TAGS="Tags";
    private static final long TAGS_ID=0;

    public static final String DB_TABLE_FAVORITE_TRACKS="FavoriteTracks";
    public static final String TABLE_FAVORITE_TRACK_ID="Id";
    public static final String TABLE_FAVORITE_TRACK_TITLE="Title";
    public static final String TABLE_FAVORITE_TRACK_DURATION="Duration";
    public static final String TABLE_FAVORITE_TRACK_PATH_ON_PHONE="PathOnPhone";


    // ------- Class methods
    public AppDBHandler(Context context, String defaultTags) {
        super(context, DB_NAME, null, DB_VERSION);

        String tags = getTags();
        if (tags.isEmpty()) {
            // DB is not initialized:
            ContentValues data = new ContentValues();
            data.put(TABLE_SETTINGS_ID, TAGS_ID);
            data.put(TABLE_SETTINGS_TAGS, defaultTags);
            insertDataInTable(DB_TABLE_SETTINGS, data);
        }

    }

    public AppDBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

//    public void resetTags() {
//        ContentValues data = new ContentValues();
//        data.put(TABLE_SETTINGS_ID, TAGS_ID);
//        data.put(TABLE_SETTINGS_TAGS, mDefaultTags);
//        rewriteDataInTable(DB_TABLE_SETTINGS,data);
//    }

    public String getTags() {
        Cursor data = getDataFromTable(
                DB_TABLE_SETTINGS,
                new String[]{TABLE_SETTINGS_ID, TABLE_SETTINGS_TAGS},
                null,
                null,
                null
        );

        if (data == null) return "";

        if (data.getCount() < 1) {
            return "";
        }

        data.moveToFirst();

        long tags_id = data.getLong(
                data.getColumnIndexOrThrow(TABLE_SETTINGS_ID)
        );
        Timber.v("DB Settings : count=" + data.getCount() + ", tags_id=" + tags_id);

        String tags = data.getString(
                data.getColumnIndexOrThrow(TABLE_SETTINGS_TAGS)
        );
        Timber.v("Get tags : " + tags);
        return tags;
    }

    public void setTags(String tags) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues data = new ContentValues();
        data.put(TABLE_SETTINGS_TAGS, tags);
        db.update(DB_TABLE_SETTINGS,
                data,
                TABLE_SETTINGS_ID + "=" + String.valueOf(TAGS_ID),
                null);
    }

    /*
    public Cursor getAllDataFromTable(String tableName) {

        if (mHandlerPrivate == null) {
            return null;
        }
        // Field names can be null -> all fields are given, however it is discouraged
        // http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html
        String[] fieldNames = null;
        return mHandlerPrivate.getReadableDatabase().query(tableName,
                fieldNames,
                null,
                null,
                null,
                null,
                null);
    }

    public Cursor getAllDataFromTable(String tableName, String[] fieldNames) {

        if (mHandlerPrivate == null) {
            return null;
        }
        // Field names can be null -> all fields are given, however it is discouraged
        // http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html
        return mHandlerPrivate.getReadableDatabase().query(tableName,
                fieldNames,
                null,
                null,
                null,
                null,
                null);
    }


    public Cursor getDataOnRowIdFromTable(long id, String tableName) {
        if (mHandlerPrivate == null) {
            return null;
        }
        return mHandlerPrivate.getReadableDatabase().query(
                true,
                tableName,
                null, // field names
                GeoDBConf.COMMON_KEY_ID + "=" + String.valueOf(id),
                null,
                null,
                null,
                null,
                null
        );

    }


    public long createDataInTable(String tableName, ContentValues data) {
        SQLiteDatabase db = mHandlerPrivate.getWritableDatabase();
        return db.insert(tableName, null, data);
    }


    public boolean updateDataInTable(String tableName, long id, ContentValues data) {
        SQLiteDatabase db = mHandlerPrivate.getWritableDatabase();
        return db.update(
                tableName,
                data,
                GeoDBConf.COMMON_KEY_ID + "=" + String.valueOf(id),
                null) > 0;

    }

    public boolean deleteDataInTable(String tableName, long id)
    {
        SQLiteDatabase db = mHandlerPrivate.getWritableDatabase();
        return db.delete(
                tableName,
                GeoDBConf.COMMON_KEY_ID + "=" + String.valueOf(id),
                null) > 0;
    }


    public boolean deleteDataInTable(String tableName, long[] ids)
    {
        SQLiteDatabase db = mHandlerPrivate.getWritableDatabase();
        String values = "";
        for (int i=0; i<ids.length-1; i++) {
            long id = ids[i];
            values += String.valueOf(id) + ", ";
        }
        values += String.valueOf(ids[ids.length-1]);
        return db.delete(
                tableName,
                GeoDBConf.COMMON_KEY_ID + " in (" + values + ")",
                null) > 0;
    }
    */

    /// Method to rewrite DB table data : delete all DB rows and insert new rows
    public long rewriteDataInTable(String tableName, ContentValues data) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(tableName, null, null);
        return db.insert(tableName, null, data);
    }

    /// Method to insert new data in DB table
    public long insertDataInTable(String tableName, ContentValues data) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(tableName, null, data);
    }

    public Cursor getDataFromTable(String tableName,
                                   String[] columns,
                                   String selection,
                                   String[] selectionArgs,
                                   String orderBy) {
        return getReadableDatabase().query(
                true,
                tableName,
                columns, // field names:
                selection, // selection
                selectionArgs, // args of selection
                null, // groupBy
                null, // having
                orderBy, // orderBy
                null // limit
        );
    }

    // ----- SQLiteOpenHelper extension
    @Override
    public void onCreate(SQLiteDatabase db) {
        Timber.v("onCreate db");
        db.execSQL(createSettingsTableQuery());
        db.execSQL(createFavoriteTracksTableQuery());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.v("onUpgrade db from " + String.valueOf(oldVersion) + " to " + String.valueOf(newVersion));
        db.execSQL(dropTableQuery(DB_TABLE_SETTINGS));
        db.execSQL(dropTableQuery(DB_TABLE_FAVORITE_TRACKS));
        onCreate(db);
    }


    // ----- Other methods
    /// Method composes the query to create a table
    private String createSettingsTableQuery() {
        StringBuilder out = new StringBuilder("CREATE TABLE ")
                .append(DB_TABLE_SETTINGS).append("(");
        out.append(TABLE_SETTINGS_ID).append(" integer primary key, ");
        out.append(TABLE_SETTINGS_TAGS).append(" text non null");
        out.append(");");
        return out.toString();
    }

    private String createFavoriteTracksTableQuery() {
        StringBuilder out = new StringBuilder("CREATE TABLE ")
                .append(DB_TABLE_FAVORITE_TRACKS).append("(");
        out.append(TABLE_FAVORITE_TRACK_ID).append(" integer primary key, ");
        out.append(TABLE_FAVORITE_TRACK_TITLE).append(" text non null, ");
        out.append(TABLE_FAVORITE_TRACK_DURATION).append(" integer, ");
        out.append(TABLE_FAVORITE_TRACK_PATH_ON_PHONE).append(" string");
        out.append(");");
        return out.toString();
    }

    /// Method composes the query to drop a table
    private String dropTableQuery(String tableName) {
        return "DROP TABLE IF EXISTS " + tableName + ";";
    }


}