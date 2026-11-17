package com.example.druga_gr2;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ListaOsob {
    private static ListaOsob mLista;
    private final SQLiteDatabase database;

    private static final String SELECT_OSOBY = "SELECT o.OsobaUuid AS OsobaUuid, "
            + "o.Imie AS Imie, o.Nazwisko AS Nazwisko, a.AdresUuid AS AdresUuid, "
            + "a.Miasto AS Miasto, a.Ulica AS Ulica, a.NumerDomu AS NumerDomu, "
            + "a.KodPocztowy AS KodPocztowy FROM Osoby o "
            + "LEFT JOIN Adres a ON o.AdresUuid = a.AdresUuid";

    private ListaOsob(Context context) {
        this(new OsobyDBH(context.getApplicationContext()).getWritableDatabase());
    }

    ListaOsob(SQLiteDatabase database) {
        this.database = database;
    }

    public static synchronized ListaOsob get(Context context) {
        if (mLista == null) {
            mLista = new ListaOsob(context);
        }
        return mLista;
    }

    public synchronized List<Osoba> pobierzOsoby() {
        List<Osoba> osoby = new ArrayList<>();
        try (OsobyCursorWrapper cursor = new OsobyCursorWrapper(
                database.rawQuery(SELECT_OSOBY + " ORDER BY o._id", null))) {
            while (cursor.moveToNext()) {
                osoby.add(cursor.getOsoba());
            }
        }
        return osoby;
    }

    public synchronized Osoba pobierzOsobe(String uuid) {
        sprawdzUuid(uuid);
        try (OsobyCursorWrapper cursor = new OsobyCursorWrapper(
                database.rawQuery(SELECT_OSOBY + " WHERE o.OsobaUuid = ?", new String[]{uuid}))) {
            return cursor.moveToFirst() ? cursor.getOsoba() : null;
        }
    }

    public synchronized void dodajOsobe(Osoba osoba) {
        database.beginTransaction();
        try {
            if (osoba.getAdres() != null) {
                database.insertOrThrow(OsobyDB.AdresTabela.Name, null,
                        getContentValuesAdres(osoba.getAdres()));
            }
            database.insertOrThrow(OsobyDB.OsobyTabela.Name, null, getContentValuesOsoba(osoba));
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public synchronized void zmienOsobe(Osoba osoba) {
        database.beginTransaction();
        try {
            Osoba poprzednia = wymaganaOsoba(osoba.getmIdOsoba());
            String staryAdres = poprzednia.getAdres() == null
                    ? null : poprzednia.getAdres().getIdAdres();
            String nowyAdres = osoba.getAdres() == null ? null : osoba.getAdres().getIdAdres();

            if (osoba.getAdres() != null) {
                if (Objects.equals(staryAdres, nowyAdres)) {
                    zmienAdres(osoba.getAdres());
                } else {
                    database.insertOrThrow(OsobyDB.AdresTabela.Name, null,
                            getContentValuesAdres(osoba.getAdres()));
                }
            }
            wymaganyJeden(database.update(OsobyDB.OsobyTabela.Name,
                    getContentValuesOsoba(osoba), "OsobaUuid = ?",
                    new String[]{osoba.getmIdOsoba()}));
            if (staryAdres != null && !Objects.equals(staryAdres, nowyAdres)) {
                usunAdres(staryAdres);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public synchronized void usunOsobe(String uuid) {
        database.beginTransaction();
        try {
            Osoba osoba = wymaganaOsoba(uuid);
            wymaganyJeden(database.delete(OsobyDB.OsobyTabela.Name,
                    "OsobaUuid = ?", new String[]{uuid}));
            if (osoba.getAdres() != null) {
                usunAdres(osoba.getAdres().getIdAdres());
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public synchronized void zmienAdres(Adres adres) {
        wymaganyJeden(database.update(OsobyDB.AdresTabela.Name,
                getContentValuesAdres(adres), "AdresUuid = ?", new String[]{adres.getIdAdres()}));
    }

    public synchronized void usunAdres(String uuid) {
        sprawdzUuid(uuid);
        wymaganyJeden(database.delete(OsobyDB.AdresTabela.Name,
                "AdresUuid = ?", new String[]{uuid}));
    }

    public static ContentValues getContentValuesOsoba(Osoba osoba) {
        sprawdzUuid(osoba.getmIdOsoba());
        ContentValues values = new ContentValues();
        values.put(OsobyDB.OsobyTabela.Kolumny.UUID, osoba.getmIdOsoba());
        values.put(OsobyDB.OsobyTabela.Kolumny.IMIE, osoba.getImie());
        values.put(OsobyDB.OsobyTabela.Kolumny.NAZWISKO, osoba.getNazwisko());
        if (osoba.getAdres() == null) {
            values.putNull(OsobyDB.OsobyTabela.Kolumny.ADRES_UUID);
        } else {
            sprawdzUuid(osoba.getAdres().getIdAdres());
            values.put(OsobyDB.OsobyTabela.Kolumny.ADRES_UUID, osoba.getAdres().getIdAdres());
        }
        return values;
    }

    public static ContentValues getContentValuesAdres(Adres adres) {
        sprawdzUuid(adres.getIdAdres());
        ContentValues values = new ContentValues();
        values.put(OsobyDB.AdresTabela.Kolumny.UUID, adres.getIdAdres());
        values.put(OsobyDB.AdresTabela.Kolumny.MIASTO, adres.getMiasto());
        values.put(OsobyDB.AdresTabela.Kolumny.ULICA, adres.getUlica());
        values.put(OsobyDB.AdresTabela.Kolumny.NUMER_DOMU, adres.getNumerDomu());
        values.put(OsobyDB.AdresTabela.Kolumny.KOD_POCZTOWY, adres.getKodPocztowy());
        return values;
    }

    private Osoba wymaganaOsoba(String uuid) {
        Osoba osoba = pobierzOsobe(uuid);
        if (osoba == null) {
            throw new SQLiteException("Osoba nie istnieje: " + uuid);
        }
        return osoba;
    }

    private static void wymaganyJeden(int count) {
        if (count != 1) {
            throw new SQLiteException("Oczekiwano zmiany jednego rekordu, zmieniono: " + count);
        }
    }

    private static void sprawdzUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            throw new IllegalArgumentException("UUID nie może być pusty");
        }
    }
}
