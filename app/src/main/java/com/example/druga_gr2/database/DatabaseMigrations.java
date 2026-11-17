package com.example.druga_gr2.database;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DatabaseMigrations {
    private DatabaseMigrations() { }

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE Adres (AdresUuid TEXT PRIMARY KEY, Miasto TEXT, "
                    + "Ulica TEXT, NumerDomu TEXT, KodPocztowy TEXT)");
            db.execSQL("ALTER TABLE Osoby ADD COLUMN AdresUuid TEXT "
                    + "REFERENCES Adres(AdresUuid) ON DELETE SET NULL");
            uzupelnijUuid(db, "Osoby", "OsobaUuid");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            uzupelnijUuid(db, "Osoby", "OsobaUuid");
            uzupelnijUuid(db, "Adres", "AdresUuid");
            long sequence = 0;
            try (Cursor cursor = db.query("SELECT seq FROM sqlite_sequence WHERE name = 'Osoby'")) {
                if (cursor.moveToFirst()) sequence = cursor.getLong(0);
            }
            db.execSQL("CREATE TABLE Adres_room_new (AdresUuid TEXT NOT NULL PRIMARY KEY, "
                    + "Miasto TEXT, Ulica TEXT, NumerDomu TEXT, KodPocztowy TEXT)");
            db.execSQL("INSERT INTO Adres_room_new SELECT AdresUuid, Miasto, Ulica, "
                    + "NumerDomu, KodPocztowy FROM Adres");
            db.execSQL("CREATE TABLE Osoby_room_copy AS SELECT _id, OsobaUuid, Imie, "
                    + "Nazwisko, AdresUuid FROM Osoby");
            db.execSQL("DROP TABLE Osoby");
            db.execSQL("DROP TABLE Adres");
            db.execSQL("ALTER TABLE Adres_room_new RENAME TO Adres");
            db.execSQL("CREATE TABLE Osoby (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "OsobaUuid TEXT NOT NULL, Imie TEXT, Nazwisko TEXT, AdresUuid TEXT, "
                    + "FOREIGN KEY(AdresUuid) REFERENCES Adres(AdresUuid) "
                    + "ON UPDATE NO ACTION ON DELETE SET NULL)");
            db.execSQL("INSERT INTO Osoby SELECT _id, OsobaUuid, Imie, Nazwisko, AdresUuid "
                    + "FROM Osoby_room_copy");
            db.execSQL("DROP TABLE Osoby_room_copy");
            db.execSQL("CREATE UNIQUE INDEX index_Osoby_OsobaUuid ON Osoby(OsobaUuid)");
            db.execSQL("CREATE INDEX index_Osoby_AdresUuid ON Osoby(AdresUuid)");
            db.execSQL("INSERT INTO sqlite_sequence(name, seq) SELECT 'Osoby', ? "
                    + "WHERE NOT EXISTS (SELECT 1 FROM sqlite_sequence WHERE name = 'Osoby')",
                    new Object[]{sequence});
            db.execSQL("UPDATE sqlite_sequence SET seq = MAX(seq, ?) WHERE name = 'Osoby'",
                    new Object[]{sequence});
            try (Cursor cursor = db.query("PRAGMA foreign_key_check")) {
                if (cursor.moveToFirst()) throw new IllegalStateException("Niepoprawne powiązania adresów");
            }
        }
    };

    private static void uzupelnijUuid(SupportSQLiteDatabase db, String tabela, String kolumna) {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = db.query("SELECT rowid FROM " + tabela + " WHERE " + kolumna + " IS NULL")) {
            while (cursor.moveToNext()) ids.add(cursor.getLong(0));
        }
        for (long id : ids) {
            db.execSQL("UPDATE " + tabela + " SET " + kolumna + " = ? WHERE rowid = ?",
                    new Object[]{UUID.randomUUID().toString(), id});
        }
    }
}
