package com.sm.stasversion.interfaces;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sm.stasversion.classes.DBConfig;

import org.json.JSONArray;

import java.util.List;

@Dao
public interface DBConfigDao {
    @Query("SELECT count(*) FROM DBConfig")
    Integer size();

    @Query("SELECT id FROM DBConfig ORDER BY id DESC LIMIT 1")
    Integer lastId();

    @Query("SELECT id FROM DBConfig ORDER BY id DESC")
    List<Integer> getConfigsIds();

    @Query("SELECT * FROM DBConfig ORDER BY id DESC")
    List<DBConfig> getAll();

    @Query("SELECT * FROM DBConfig WHERE name = :name")
    DBConfig getConfig(String name);

    @Query("UPDATE DBConfig SET correction = :correction, crop = :crop WHERE id = :id")
    void updateConfig(Integer id, String correction, String crop);

    @Query("UPDATE DBConfig SET correction = :correction WHERE id = :id")
    void updateConfig(Integer id, String correction);

    @Query("DELETE FROM DBConfig WHERE id = :id")
    void deleteConfig(Integer id);

    @Insert
    void insertConfigs(List<DBConfig> orders);

    @Query("DELETE FROM DBConfig")
    void clear();
}
