package com.example.druga_gr2;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OsobyDBH extends SQLiteOpenHelper {

    static final int VERSION = 2;
    static final String DATABASE_NAME = "BazaOsob2.db";

    public OsobyDBH(Context context)
    {
        this(context, DATABASE_NAME);
    }

    OsobyDBH(Context context, String databaseName) {
        super(context, databaseName, null, VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    private void utworzTabeleAdres(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Adres (AdresUuid TEXT PRIMARY KEY, Miasto TEXT, "
                + "Ulica TEXT, NumerDomu TEXT, KodPocztowy TEXT)");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        utworzTabeleAdres(db);
        db.execSQL("CREATE TABLE Osoby (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "OsobaUuid TEXT UNIQUE NOT NULL, Imie TEXT, Nazwisko TEXT, "
                + "AdresUuid TEXT REFERENCES Adres(AdresUuid) ON DELETE SET NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            utworzTabeleAdres(db);
            db.execSQL("ALTER TABLE Osoby ADD COLUMN AdresUuid TEXT "
                    + "REFERENCES Adres(AdresUuid) ON DELETE SET NULL");

            List<Long> bezUuid = new ArrayList<>();
            try (Cursor cursor = db.query("Osoby", new String[]{"_id"},
                    "OsobaUuid IS NULL", null, null, null, null)) {
                while (cursor.moveToNext()) {
                    bezUuid.add(cursor.getLong(0));
                }
            }
            for (long id : bezUuid) {
                ContentValues values = new ContentValues();
                values.put("OsobaUuid", UUID.randomUUID().toString());
                if (db.update("Osoby", values, "_id = ?", new String[]{Long.toString(id)}) != 1) {
                    throw new SQLiteException("Nie udało się nadać UUID osobie " + id);
                }
            }
            db.execSQL("CREATE TRIGGER osoby_uuid_not_null_insert BEFORE INSERT ON Osoby "
                    + "WHEN NEW.OsobaUuid IS NULL BEGIN "
                    + "SELECT RAISE(ABORT, 'OsobaUuid nie moze byc NULL'); END");
            db.execSQL("CREATE TRIGGER osoby_uuid_not_null_update BEFORE UPDATE ON Osoby "
                    + "WHEN NEW.OsobaUuid IS NULL BEGIN "
                    + "SELECT RAISE(ABORT, 'OsobaUuid nie moze byc NULL'); END");
        }
    }
}
