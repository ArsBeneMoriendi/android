package com.example.druga_gr2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.druga_gr2.database.AppDatabase;
import com.example.druga_gr2.database.DatabaseMigrations;
import com.example.druga_gr2.database.OsobaEntity;
import com.example.druga_gr2.database.OsobaZAdresem;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RoomMigrationTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private final String nazwa = "room-migracja-" + UUID.randomUUID() + ".db";
    private AppDatabase baza;

    @Rule public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), AppDatabase.class);

    @After public void zamknij() {
        if (baza != null) baza.close();
        context.deleteDatabase(nazwa);
    }

    private SQLiteDatabase staraBaza(int wersja, boolean poAktualizacji) {
        SQLiteDatabase db = context.openOrCreateDatabase(nazwa, Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE Osoby (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "OsobaUuid TEXT UNIQUE" + (wersja == 2 && !poAktualizacji ? " NOT NULL" : "")
                + ", Imie TEXT, Nazwisko TEXT)");
        if (wersja == 2) {
            db.execSQL("CREATE TABLE Adres (AdresUuid TEXT PRIMARY KEY, Miasto TEXT, "
                    + "Ulica TEXT, NumerDomu TEXT, KodPocztowy TEXT)");
            db.execSQL("ALTER TABLE Osoby ADD COLUMN AdresUuid TEXT "
                    + "REFERENCES Adres(AdresUuid) ON DELETE SET NULL");
            if (poAktualizacji) {
                for (String operacja : new String[]{"INSERT", "UPDATE"}) {
                    db.execSQL("CREATE TRIGGER osoby_uuid_not_null_" + operacja.toLowerCase()
                            + " BEFORE " + operacja + " ON Osoby WHEN NEW.OsobaUuid IS NULL "
                            + "BEGIN SELECT RAISE(ABORT, 'NULL UUID'); END");
                }
            }
        }
        db.setVersion(wersja);
        return db;
    }

    private void otworzRoom() {
        baza = Room.databaseBuilder(context, AppDatabase.class, nazwa)
                .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3)
                .build();
    }

    @Test public void migrujeSwiezaWersje2ZachowujacDaneIndeksyIRelacje() throws Exception {
        sprawdzMigracje2(false);
    }

    @Test public void migrujeWersje2PoDawnejAktualizacjiZTriggerami() throws Exception {
        sprawdzMigracje2(true);
    }

    private void sprawdzMigracje2(boolean poAktualizacji) throws Exception {
        try (SQLiteDatabase db = staraBaza(2, poAktualizacji)) {
            db.execSQL("INSERT INTO Adres VALUES ('adres-1', 'Łódź', 'Długa', '7A', '90-001')");
            db.execSQL("INSERT INTO Adres VALUES ('zachowany-nieuzywany', NULL, NULL, NULL, NULL)");
            db.execSQL("INSERT INTO Osoby VALUES (7, 'osoba-1', 'Jan', NULL, 'adres-1')");
            db.execSQL("INSERT INTO Osoby VALUES (9, 'osoba-2', NULL, 'Nowak', NULL)");
            db.execSQL("INSERT INTO Osoby VALUES (12, 'osoba-3', 'Anna', 'Nowak', 'adres-1')");
            db.execSQL("INSERT INTO Osoby VALUES (100, 'usunieta', NULL, NULL, NULL)");
            db.execSQL("DELETE FROM Osoby WHERE _id = 100");
        }
        helper.runMigrationsAndValidate(nazwa, 3, true,
                DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3).close();
        otworzRoom();
        List<OsobaZAdresem> osoby = baza.osobaDao().pobierzOsobyZAdresami();
        assertEquals(3, osoby.size());
        assertEquals(7, osoby.get(0).osoba.id);
        assertEquals("osoba-1", osoby.get(0).osoba.uuid);
        assertNull(osoby.get(0).osoba.nazwisko);
        assertEquals("adres-1", osoby.get(0).adres.uuid);
        assertEquals("Łódź", osoby.get(0).adres.miasto);
        assertEquals("7A", osoby.get(0).adres.numerDomu);
        assertNull(osoby.get(1).adres);
        assertEquals("adres-1", osoby.get(2).adres.uuid);
        assertNotNull(baza.adresDao().pobierzAdres("zachowany-nieuzywany"));
        OsobaEntity nowa = new OsobaEntity();
        nowa.uuid = "kolejna";
        assertTrue(baza.osobaDao().dodajOsobe(nowa) > 100);
        assertEquals(1, baza.adresDao().usunAdres(baza.adresDao().pobierzAdres("adres-1")));
        assertNull(baza.osobaDao().pobierzOsobe("osoba-1").adresUuid);
        assertNull(baza.osobaDao().pobierzOsobe("osoba-3").adresUuid);
        baza.close();
        otworzRoom();
        assertEquals(4, baza.osobaDao().pobierzOsoby().size());
        assertNull(baza.osobaDao().pobierzOsobeZAdresem("osoba-1").adres);
    }

    @Test public void migrujeWersje1INadajeBrakujaceUuidBezUtratyOsob() throws Exception {
        try (SQLiteDatabase db = staraBaza(1, false)) {
            db.execSQL("INSERT INTO Osoby VALUES (3, 'staly-uuid', 'Adam', 'Kowalski')");
            db.execSQL("INSERT INTO Osoby VALUES (8, NULL, 'Ewa', 'Nowak')");
            db.execSQL("INSERT INTO Osoby VALUES (9, NULL, NULL, NULL)");
        }
        helper.runMigrationsAndValidate(nazwa, 3, true,
                DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3).close();
        otworzRoom();
        List<OsobaZAdresem> osoby = baza.osobaDao().pobierzOsobyZAdresami();
        assertEquals(3, osoby.size());
        assertEquals("staly-uuid", osoby.get(0).osoba.uuid);
        assertEquals(8, osoby.get(1).osoba.id);
        assertEquals("Ewa", osoby.get(1).osoba.imie);
        assertNotEquals(osoby.get(1).osoba.uuid, osoby.get(2).osoba.uuid);
        UUID.fromString(osoby.get(1).osoba.uuid);
        UUID.fromString(osoby.get(2).osoba.uuid);
        for (OsobaZAdresem osoba : osoby) assertNull(osoba.adres);
    }

    @Test public void zachowujeAdresZeStarymKluczemNullNadajacMuUuid() throws Exception {
        try (SQLiteDatabase db = staraBaza(2, false)) {
            db.execSQL("INSERT INTO Adres VALUES (NULL, 'Kraków', NULL, NULL, NULL)");
        }
        helper.runMigrationsAndValidate(nazwa, 3, true, DatabaseMigrations.MIGRATION_2_3).close();
        otworzRoom();
        try (Cursor cursor = baza.getOpenHelper().getReadableDatabase().query("SELECT * FROM Adres")) {
            assertTrue(cursor.moveToFirst());
            UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow("AdresUuid")));
            assertEquals("Kraków", cursor.getString(cursor.getColumnIndexOrThrow("Miasto")));
            assertFalse(cursor.moveToNext());
        }
    }

    @Test public void roomSamPrzejmujeStaryPlikZWlaczonymiKluczamiObcymi() {
        try (SQLiteDatabase db = staraBaza(2, false)) {
            db.setForeignKeyConstraintsEnabled(true);
            db.execSQL("INSERT INTO Adres VALUES ('adres', 'Warszawa', 'Prosta', '1', '00-001')");
            db.execSQL("INSERT INTO Osoby VALUES (42, 'osoba', 'Jan', 'Nowak', 'adres')");
        }
        otworzRoom();
        OsobaZAdresem wynik = baza.osobaDao().pobierzOsobeZAdresem("osoba");
        assertEquals(42, wynik.osoba.id);
        assertEquals("adres", wynik.adres.uuid);
        try (Cursor cursor = baza.getOpenHelper().getReadableDatabase().query("PRAGMA foreign_keys")) {
            assertTrue(cursor.moveToFirst());
            assertEquals(1, cursor.getInt(0));
        }
        baza.close();
        otworzRoom();
        assertEquals("Warszawa", baza.osobaDao().pobierzOsobeZAdresem("osoba").adres.miasto);
    }

    @Test public void pustaStaraBazaZachowujeNumeracjePoUsunietychRekordach() throws Exception {
        try (SQLiteDatabase db = staraBaza(2, false)) {
            db.execSQL("INSERT INTO Osoby VALUES (500, 'usunieta', NULL, NULL, NULL)");
            db.execSQL("DELETE FROM Osoby");
        }
        helper.runMigrationsAndValidate(nazwa, 3, true, DatabaseMigrations.MIGRATION_2_3).close();
        otworzRoom();
        assertTrue(baza.osobaDao().pobierzOsoby().isEmpty());
        OsobaEntity osoba = new OsobaEntity();
        osoba.uuid = "pierwsza-po-migracji";
        assertTrue(baza.osobaDao().dodajOsobe(osoba) > 500);
    }

    @Test public void bladMigracjiWycofujeZmianyIZachowujeStaryPlik() {
        try (SQLiteDatabase db = staraBaza(2, false)) {
            db.execSQL("INSERT INTO Osoby VALUES (5, 'uszkodzone-powiazanie', 'Jan', 'Nowak', 'brak')");
        }
        otworzRoom();
        assertThrows(RuntimeException.class, () -> baza.osobaDao().pobierzOsoby());
        baza.close();
        baza = null;
        try (SQLiteDatabase db = context.openOrCreateDatabase(nazwa, Context.MODE_PRIVATE, null);
             Cursor cursor = db.rawQuery("SELECT * FROM Osoby", null)) {
            assertEquals(2, db.getVersion());
            assertTrue(cursor.moveToFirst());
            assertEquals("brak", cursor.getString(cursor.getColumnIndexOrThrow("AdresUuid")));
            assertEquals(5, cursor.getLong(cursor.getColumnIndexOrThrow("_id")));
        }
    }
}
