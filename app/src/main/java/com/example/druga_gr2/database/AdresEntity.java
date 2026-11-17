package com.example.druga_gr2.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Adres")
public class AdresEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "AdresUuid")
    public String uuid = "";

    @Nullable @ColumnInfo(name = "Miasto") public String miasto;
    @Nullable @ColumnInfo(name = "Ulica") public String ulica;
    @Nullable @ColumnInfo(name = "NumerDomu") public String numerDomu;
    @Nullable @ColumnInfo(name = "KodPocztowy") public String kodPocztowy;
}
