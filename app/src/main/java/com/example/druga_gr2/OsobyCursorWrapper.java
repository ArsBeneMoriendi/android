package com.example.druga_gr2;

import android.database.Cursor;
import android.database.CursorWrapper;

public class OsobyCursorWrapper extends CursorWrapper {
    public OsobyCursorWrapper(Cursor cursor) {
        super(cursor);
    }
    public Osoba getOsoba(){
        String uuid = getString(getColumnIndexOrThrow(OsobyDB.OsobyTabela.Kolumny.UUID));
        String imie = getString(getColumnIndexOrThrow(OsobyDB.OsobyTabela.Kolumny.IMIE));
        String nazwisko = getString(getColumnIndexOrThrow(OsobyDB.OsobyTabela.Kolumny.NAZWISKO));

        Osoba osoba = new Osoba();
        osoba.setmIdOsoba(uuid);
        osoba.setImie(imie);
        osoba.setNazwisko(nazwisko);
        int adresUuid = getColumnIndexOrThrow(OsobyDB.AdresTabela.Kolumny.UUID);
        if (!isNull(adresUuid)) {
            Adres adres = new Adres(
                    getString(getColumnIndexOrThrow(OsobyDB.AdresTabela.Kolumny.MIASTO)),
                    getString(getColumnIndexOrThrow(OsobyDB.AdresTabela.Kolumny.ULICA)),
                    getString(getColumnIndexOrThrow(OsobyDB.AdresTabela.Kolumny.NUMER_DOMU)),
                    getString(getColumnIndexOrThrow(OsobyDB.AdresTabela.Kolumny.KOD_POCZTOWY)));
            adres.setIdAdres(getString(adresUuid));
            osoba.setAdres(adres);
        }
        return osoba;
    }
}
