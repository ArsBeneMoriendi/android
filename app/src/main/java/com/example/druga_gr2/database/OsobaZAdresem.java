package com.example.druga_gr2.database;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Relation;

public class OsobaZAdresem {
    @Embedded public OsobaEntity osoba;

    @Nullable
    @Relation(parentColumn = "AdresUuid", entityColumn = "AdresUuid")
    public AdresEntity adres;
}
