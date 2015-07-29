package com.vfdev.gettingthingsdonemusicapp.DB;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import timber.log.Timber;

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

     // name of the database file for your application -- change to something appropriate for your app
     private static final String DATABASE_NAME = "tracks.db";
     // any time you make changes to your database objects, you may have to increase the database version
     private static final int DATABASE_VERSION = 2;

     // the DAO object we use to access the SimpleData table
     private Dao<DBTrackInfo, String> trackInfoDao = null;
     private RuntimeExceptionDao<DBTrackInfo, String> trackInfoREDao = null;

     public DatabaseHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
     }

     /**
      * This is called when the database is first created. Usually you should call createTable statements here to create
      * the tables that will store your data.
      */
     @Override
     public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
         try {
             Timber.v("onCreate");
             TableUtils.createTable(connectionSource, DBTrackInfo.class);

         } catch (SQLException e) {
             Timber.e("Can't create database", e);
             throw new RuntimeException(e);
         }
     }

     /**
      * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
      * the various data to match the new version number.
      */
     @Override
     public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
         try {
             Timber.v("onUpgrade");
             TableUtils.dropTable(connectionSource, DBTrackInfo.class, true);
             // after we drop the old databases, we create the new ones
             onCreate(db, connectionSource);
         } catch (SQLException e) {
             Timber.e("Can't drop databases", e);
             throw new RuntimeException(e);
         }
     }

     /**
      * Returns the Database Access Object (DAO) for DBTrackInfo class. It will create it or just give the cached
      * value.
      */
     public Dao<DBTrackInfo, String> getTrackInfoDao() throws SQLException {
         if (trackInfoDao == null) {
             trackInfoDao = getDao(DBTrackInfo.class);
         }
         return trackInfoDao;
     }

     /**
      * Returns the RuntimeExceptionDao (Database Access Object) version of a Dao for our SimpleData class. It will
      * create it or just give the cached value. RuntimeExceptionDao only through RuntimeExceptions.
      */
     public RuntimeExceptionDao<DBTrackInfo, String> getTrackInfoREDao() {
         if (trackInfoREDao == null) {
             trackInfoREDao = getRuntimeExceptionDao(DBTrackInfo.class);
         }
         return trackInfoREDao;
     }

     /**
      * Close the database connections and clear any cached DAOs.
      */
     @Override
     public void close() {
         super.close();
         trackInfoDao = null;
         trackInfoREDao = null;
     }
 }