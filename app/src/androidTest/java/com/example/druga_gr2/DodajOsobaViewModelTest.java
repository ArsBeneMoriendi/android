package com.example.druga_gr2;

import androidx.lifecycle.SavedStateHandle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DodajOsobaViewModelTest {
    private final KontrolowanaLista lista = new KontrolowanaLista();

    @After public void zamknij() { lista.close(); }

    private void main(Runnable test) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(test);
    }

    @Test public void blokujeEdycjePodczasOdczytuISpoznionyWynikNieNadpisujeZmian() {
        main(() -> {
            DodajOsobaViewModel model = new DodajOsobaViewModel(new SavedStateHandle());
            Osoba osoba = new Osoba();
            osoba.setImie("Z bazy");
            model.inicjuj(lista, osoba.getmIdOsoba());
            assertFalse(model.moznaEdytowac());
            model.ustawPola("Przed odczytem", "");
            model.zapisz();
            assertEquals(0, lista.zapisy);
            lista.odczyt.onSuccess(osoba);
            assertEquals("Z bazy", model.imie());
            model.ustawPola("Robocze", "Zmiany");
            model.inicjuj(lista, osoba.getmIdOsoba());
            model.wczytaj();
            assertEquals(1, lista.odczyty);
            lista.odczyt.onSuccess(osoba);
            assertEquals("Robocze", model.imie());
            assertEquals("Zmiany", model.nazwisko());
        });
    }

    @Test public void blokujeWielokrotnyZapisABladZachowujeFormularzIPozwalaPonowic() {
        main(() -> {
            DodajOsobaViewModel model = new DodajOsobaViewModel(new SavedStateHandle());
            model.inicjuj(lista, null);
            model.ustawPola("Jan", "Nowak");
            Adres adres = new Adres("Łódź", "Długa", "1", "90-001");
            model.ustawAdres(adres);
            model.zapisz();
            model.zapisz();
            model.usun();
            assertEquals(1, lista.zapisy);
            assertTrue(model.zajety());
            lista.zapis.onError(new IllegalStateException("Kontrolowany błąd"));
            assertFalse(model.ukonczono());
            assertTrue(model.moznaEdytowac());
            assertEquals("Jan", model.imie());
            assertEquals(adres.getIdAdres(), model.adres().getIdAdres());
            assertEquals(R.string.blad_zapisu, model.odbierzKomunikat());
            assertEquals(0, model.odbierzKomunikat());
            model.zapisz();
            assertEquals(2, lista.zapisy);
            lista.zapis.onSuccess(null);
            assertTrue(model.ukonczono());
        });
    }

    @Test public void przywracaSzkicIUuidZeSavedStateBezPonownegoOdczytu() {
        main(() -> {
            SavedStateHandle stan = new SavedStateHandle();
            DodajOsobaViewModel pierwszy = new DodajOsobaViewModel(stan);
            Osoba osoba = new Osoba();
            pierwszy.inicjuj(lista, osoba.getmIdOsoba());
            lista.odczyt.onSuccess(osoba);
            pierwszy.ustawPola("Robocze", "Nazwisko");
            Adres adres = new Adres("Kraków", "Prosta", "9", "30-001");
            pierwszy.ustawAdres(adres);
            Map<String, Object> zapisanyStan = new HashMap<>();
            for (String klucz : stan.keys()) zapisanyStan.put(klucz, stan.get(klucz));
            DodajOsobaViewModel drugi = new DodajOsobaViewModel(new SavedStateHandle(zapisanyStan));
            drugi.inicjuj(lista, osoba.getmIdOsoba());
            assertEquals(1, lista.odczyty);
            assertEquals(osoba.getmIdOsoba(), drugi.uuid());
            assertEquals("Robocze", drugi.imie());
            assertEquals("Nazwisko", drugi.nazwisko());
            assertEquals(adres.getIdAdres(), drugi.adres().getIdAdres());
            assertTrue(drugi.edycja());
            assertTrue(drugi.moznaEdytowac());
        });
    }

    @Test public void poBledzieOdczytuPozwalaPonowicABrakOsobyNieUruchamiaZapisu() {
        main(() -> {
            DodajOsobaViewModel model = new DodajOsobaViewModel(new SavedStateHandle());
            model.inicjuj(lista, "brak");
            lista.odczyt.onError(new IllegalStateException("Kontrolowany błąd"));
            assertFalse(model.zajety());
            assertFalse(model.gotowe());
            assertEquals(R.string.blad_odczytu, model.odbierzKomunikat());
            model.wczytaj();
            assertEquals(2, lista.odczyty);
            lista.odczyt.onSuccess(null);
            assertEquals(R.string.osoba_nie_istnieje, model.odbierzKomunikat());
            model.zapisz();
            assertEquals(0, lista.zapisy);
        });
    }
}
