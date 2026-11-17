package com.example.druga_gr2.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import java.util.List;

@Dao
public interface OsobaDao {
    @Query("SELECT * FROM Osoby ORDER BY _id")
    List<OsobaEntity> pobierzOsoby();

    @Query("SELECT * FROM Osoby WHERE OsobaUuid = :uuid")
    OsobaEntity pobierzOsobe(String uuid);

    @Transaction
    @Query("SELECT * FROM Osoby ORDER BY _id")
    List<OsobaZAdresem> pobierzOsobyZAdresami();

    @Transaction
    @Query("SELECT * FROM Osoby WHERE OsobaUuid = :uuid")
    OsobaZAdresem pobierzOsobeZAdresem(String uuid);

    @Insert long dodajOsobe(OsobaEntity osoba);
    @Update int aktualizujOsobe(OsobaEntity osoba);
    @Delete int usunOsobe(OsobaEntity osoba);
}
