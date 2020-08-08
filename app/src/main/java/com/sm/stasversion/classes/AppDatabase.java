package com.sm.stasversion.classes;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.sm.stasversion.interfaces.DBConfigDao;

@Database(entities = {DBConfig.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DBConfigDao configDao();
}
