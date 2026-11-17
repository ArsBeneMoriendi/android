package com.example.druga_gr2.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface AdresDao {
    @Query("SELECT * FROM Adres WHERE AdresUuid = :uuid")
    AdresEntity pobierzAdres(String uuid);

    @Query("SELECT a.* FROM Adres a INNER JOIN Osoby o ON o.AdresUuid = a.AdresUuid "
            + "WHERE o.OsobaUuid = :uuid")
    AdresEntity pobierzAdresOsoby(String uuid);

    @Insert long dodajAdres(AdresEntity adres);
    @Update int aktualizujAdres(AdresEntity adres);
    @Delete int usunAdres(AdresEntity adres);

    @Query("DELETE FROM Adres WHERE AdresUuid = :uuid "
            + "AND NOT EXISTS (SELECT 1 FROM Osoby WHERE AdresUuid = :uuid)")
    int usunJesliNieuzywany(String uuid);
}
