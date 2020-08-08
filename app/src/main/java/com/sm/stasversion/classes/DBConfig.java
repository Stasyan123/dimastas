package com.sm.stasversion.classes;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DBConfig {
    @PrimaryKey(autoGenerate = true)
    private Integer id;

    @ColumnInfo(name = "path")
    private String path = "";

    @ColumnInfo(name = "name")
    private String name = "";

    @ColumnInfo(name = "correction")
    private String correction = "";

    @ColumnInfo(name = "crop")
    private String crop = "";

    @NonNull
    public Integer getId() {
        return id;
    }

    public void setId(@NonNull Integer id) {
        this.id = id;
    }

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getCorrection() {
        return correction;
    }

    public void setCorrection(String correction) {
        this.correction = correction;
    }

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }


    public DBConfig() {

    }
}
