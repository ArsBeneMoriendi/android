package com.example.druga_gr2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.druga_gr2.database.AppDatabase;
import com.example.druga_gr2.database.OsobaEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static com.example.druga_gr2.RoomTestSupport.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RoomRepositoryTest {
    private final Context context = ApplicationProvider.getApplicationContext();
    private final String nazwa = "room-crud-" + UUID.randomUUID() + ".db";
    private AppDatabase baza;
    private ExecutorService executor;
    private ListaOsob lista;

    @Before public void przygotuj() {
        executor = Executors.newSingleThreadExecutor();
        otworz();
    }

    private void otworz() {
        baza = Room.databaseBuilder(context, AppDatabase.class, nazwa).build();
        lista = new ListaOsob(baza, executor, new Handler(Looper.getMainLooper()));
    }

    @After public void zamknij() throws Exception {
        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        baza.close();
        context.deleteDatabase(nazwa);
    }

    private Osoba osoba(boolean zAdresem) {
        Osoba osoba = new Osoba();
        osoba.setImie("Jan");
        osoba.setNazwisko("Kowalski");
        if (zAdresem) osoba.setAdres(new Adres("Warszawa", "Prosta", "1A", "00-001"));
        return osoba;
    }

    private Osoba pobierz(String uuid) throws Exception {
        return odczyt(cb -> lista.pobierzOsobe(uuid, cb));
    }

    @Test public void tworzyNowaBazeIDodajeOsobeBezAdresu() throws Exception {
        List<Osoba> pusta = odczyt(cb -> lista.pobierzOsoby(cb));
        assertTrue(pusta.isEmpty());
        Osoba osoba = osoba(false);
        zapis(cb -> lista.dodajOsobe(osoba, cb));
        assertEquals(osoba.getmIdOsoba(), pobierz(osoba.getmIdOsoba()).getmIdOsoba());
        assertNull(pobierz(osoba.getmIdOsoba()).getAdres());
        assertEquals(3, baza.getOpenHelper().getReadableDatabase().getVersion());
    }

    @Test public void dodajeOsobeZAdresemIEdytujeBezZmianyUuidAniId() throws Exception {
        Osoba osoba = osoba(true);
        String adresUuid = osoba.getAdres().getIdAdres();
        zapis(cb -> lista.dodajOsobe(osoba, cb));
        long id = baza.osobaDao().pobierzOsobe(osoba.getmIdOsoba()).id;
        osoba.setImie("Anna");
        osoba.setNazwisko("Nowak");
        osoba.getAdres().setMiasto("Gdańsk");
        zapis(cb -> lista.zmienOsobe(osoba, cb));
        Osoba wynik = pobierz(osoba.getmIdOsoba());
        assertEquals("Anna", wynik.getImie());
        assertEquals("Nowak", wynik.getNazwisko());
        assertEquals(adresUuid, wynik.getAdres().getIdAdres());
        assertEquals("Gdańsk", wynik.getAdres().getMiasto());
        assertEquals(id, baza.osobaDao().pobierzOsobe(osoba.getmIdOsoba()).id);
        assertEquals(adresUuid, baza.adresDao().pobierzAdresOsoby(osoba.getmIdOsoba()).uuid);
    }

    @Test public void dodajeAdresOsobieBezAdresuOrazUsuwaGoPrzezEdycje() throws Exception {
        Osoba osoba = osoba(false);
        zapis(cb -> lista.dodajOsobe(osoba, cb));
        osoba.setAdres(new Adres("Łódź", "Długa", "2", "90-001"));
        String adresUuid = osoba.getAdres().getIdAdres();
        zapis(cb -> lista.zmienOsobe(osoba, cb));
        assertEquals(adresUuid, pobierz(osoba.getmIdOsoba()).getAdres().getIdAdres());
        osoba.setAdres(null);
        zapis(cb -> lista.zmienOsobe(osoba, cb));
        assertNull(pobierz(osoba.getmIdOsoba()).getAdres());
        assertNull(baza.adresDao().pobierzAdres(adresUuid));
    }

    @Test public void zmieniaIUsuwaAdresBezUsuwaniaOsoby() throws Exception {
        Osoba osoba = osoba(true);
        zapis(cb -> lista.dodajOsobe(osoba, cb));
        Adres adres = osoba.getAdres();
        adres.setUlica("Nowa");
        zapis(cb -> lista.zmienAdres(adres, cb));
        assertEquals("Nowa", pobierz(osoba.getmIdOsoba()).getAdres().getUlica());
        assertEquals(adres.getIdAdres(), pobierz(osoba.getmIdOsoba()).getAdres().getIdAdres());
        zapis(cb -> lista.usunAdres(adres.getIdAdres(), cb));
        assertNotNull(pobierz(osoba.getmIdOsoba()));
        assertNull(pobierz(osoba.getmIdOsoba()).getAdres());
    }

    @Test public void usuwajacOsobeUsuwaJejNieuzywanyAdres() throws Exception {
        Osoba osoba = osoba(true);
        zapis(cb -> lista.dodajOsobe(osoba, cb));
        zapis(cb -> lista.usunOsobe(osoba.getmIdOsoba(), cb));
        assertNull(pobierz(osoba.getmIdOsoba()));
        assertNull(baza.adresDao().pobierzAdres(osoba.getAdres().getIdAdres()));
    }

    @Test public void zastepujeAdresISprzataPoprzedni() throws Exception {
        Osoba osoba = osoba(true);
        String stary = osoba.getAdres().getIdAdres();
        zapis(cb -> lista.dodajOsobe(osoba, cb));
        osoba.setAdres(new Adres("Kraków", "Krótka", "3", "30-001"));
        zapis(cb -> lista.zmienOsobe(osoba, cb));
        assertNull(baza.adresDao().pobierzAdres(stary));
        assertEquals(osoba.getAdres().getIdAdres(), pobierz(osoba.getmIdOsoba()).getAdres().getIdAdres());
    }

    @Test public void zachowujeWspoldzielonyAdresDoUsunieciaOstatniegoUzytkownika() throws Exception {
        Osoba pierwsza = osoba(true);
        Osoba druga = osoba(false);
        druga.setAdres(ListaOsob.kopiujAdres(pierwsza.getAdres()));
        String wspolny = pierwsza.getAdres().getIdAdres();
        zapis(cb -> lista.dodajOsobe(pierwsza, cb));
        zapis(cb -> lista.dodajOsobe(druga, cb));
        pierwsza.setAdres(new Adres("Poznań", "Inna", "5", "60-001"));
        zapis(cb -> lista.zmienOsobe(pierwsza, cb));
        assertNotNull(baza.adresDao().pobierzAdres(wspolny));
        zapis(cb -> lista.usunOsobe(pierwsza.getmIdOsoba(), cb));
        assertEquals(wspolny, pobierz(druga.getmIdOsoba()).getAdres().getIdAdres());
        zapis(cb -> lista.usunOsobe(druga.getmIdOsoba(), cb));
        assertNull(baza.adresDao().pobierzAdres(wspolny));
    }

    @Test public void konfliktOsobyWycofujeDodanieIUpdateAdresu() throws Exception {
        Osoba osoba = osoba(true);
        zapis(cb -> lista.dodajOsobe(osoba, cb));
        String staryAdres = osoba.getAdres().getIdAdres();
        osoba.getAdres().setMiasto("Nie zapisać");
        assertThrows(ExecutionException.class, () -> zapis(cb -> lista.dodajOsobe(osoba, cb)));
        assertEquals("Warszawa", pobierz(osoba.getmIdOsoba()).getAdres().getMiasto());
        osoba.setAdres(new Adres("Nowy", "Test", "1", "00-000"));
        assertThrows(ExecutionException.class, () -> zapis(cb -> lista.dodajOsobe(osoba, cb)));
        assertNull(baza.adresDao().pobierzAdres(osoba.getAdres().getIdAdres()));
        assertEquals(staryAdres, pobierz(osoba.getmIdOsoba()).getAdres().getIdAdres());
    }

    @Test public void kluczObcyOdrzucaNieistniejacyAdres() {
        OsobaEntity encja = new OsobaEntity();
        encja.uuid = "test";
        encja.adresUuid = "nieistniejacy-adres";
        assertThrows(RuntimeException.class, () -> baza.osobaDao().dodajOsobe(encja));
        assertTrue(baza.osobaDao().pobierzOsoby().isEmpty());
    }

    @Test public void obslugujeBrakRekordowNiepoprawneUuidIBrakAdresu() throws Exception {
        assertNull(pobierz("brak"));
        Osoba brak = osoba(true);
        assertThrows(ExecutionException.class, () -> zapis(cb -> lista.zmienOsobe(brak, cb)));
        assertNull(baza.adresDao().pobierzAdres(brak.getAdres().getIdAdres()));
        assertThrows(ExecutionException.class, () -> zapis(cb -> lista.usunOsobe("brak", cb)));
        assertThrows(ExecutionException.class, () -> zapis(cb -> lista.usunAdres("brak", cb)));
        assertThrows(ExecutionException.class, () -> zapis(cb -> lista.zmienAdres(brak.getAdres(), cb)));
        brak.setmIdOsoba("");
        assertThrows(ExecutionException.class, () -> zapis(cb -> lista.dodajOsobe(brak, cb)));
    }

    @Test public void zachowujeDanePoZamknieciuIPonownymOtwarciuPliku() throws Exception {
        Osoba osoba = osoba(true);
        zapis(cb -> lista.dodajOsobe(osoba, cb));
        baza.close();
        otworz();
        Osoba wynik = pobierz(osoba.getmIdOsoba());
        assertEquals("Jan", wynik.getImie());
        assertEquals(osoba.getAdres().getIdAdres(), wynik.getAdres().getIdAdres());
    }

    @Test public void kopiujeDanePrzedZakolejkowaniemZapisu() throws Exception {
        CountDownLatch odblokuj = new CountDownLatch(1);
        executor.execute(() -> {
            try { odblokuj.await(10, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Osoba osoba = osoba(true);
        java.util.concurrent.CompletableFuture<Void> zapisano = new java.util.concurrent.CompletableFuture<>();
        lista.dodajOsobe(osoba, new ListaOsob.Callback<Void>() {
            @Override public void onSuccess(Void wynik) { zapisano.complete(null); }
            @Override public void onError(Exception blad) { zapisano.completeExceptionally(blad); }
        });
        osoba.setImie("Późniejsza zmiana");
        osoba.getAdres().setMiasto("Późniejsza zmiana");
        odblokuj.countDown();
        zapisano.get(15, TimeUnit.SECONDS);
        assertEquals("Jan", pobierz(osoba.getmIdOsoba()).getImie());
        assertEquals("Warszawa", pobierz(osoba.getmIdOsoba()).getAdres().getMiasto());
    }

    @Test public void apiWywolaneNaGlownymWatkuKorzystaZPracownikaARoomBlokujeBezposredniOdczyt() throws Exception {
        Osoba osoba = osoba(false);
        zapis(cb -> InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> lista.dodajOsobe(osoba, cb)));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                assertThrows(IllegalStateException.class, () -> baza.osobaDao().pobierzOsoby()));
        assertNotNull(pobierz(osoba.getmIdOsoba()));
    }
}
