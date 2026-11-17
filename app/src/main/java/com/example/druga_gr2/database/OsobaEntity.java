package com.example.druga_gr2.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "Osoby",
        foreignKeys = @ForeignKey(entity = AdresEntity.class,
                parentColumns = "AdresUuid", childColumns = "AdresUuid",
                onDelete = ForeignKey.SET_NULL),
        indices = {@Index(value = "OsobaUuid", unique = true), @Index("AdresUuid")})
public class OsobaEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long id;

    @NonNull @ColumnInfo(name = "OsobaUuid") public String uuid = "";
    @Nullable @ColumnInfo(name = "Imie") public String imie;
    @Nullable @ColumnInfo(name = "Nazwisko") public String nazwisko;
    @Nullable @ColumnInfo(name = "AdresUuid") public String adresUuid;
}
