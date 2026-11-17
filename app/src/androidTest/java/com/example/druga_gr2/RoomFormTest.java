package com.example.druga_gr2;

import android.os.SystemClock;
import android.widget.ListView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import com.example.druga_gr2.database.AppDatabase;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.example.druga_gr2.RoomTestSupport.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RoomFormTest {
    private final ListaOsob lista = ListaOsob.get(ApplicationProvider.getApplicationContext());
    private final List<String> testoweUuid = new ArrayList<>();

    @After public void sprzatajWylacznieDaneTestowe() throws Exception {
        for (String uuid : testoweUuid) {
            Osoba osoba = odczyt(cb -> lista.pobierzOsobe(uuid, cb));
            if (osoba != null) zapis(cb -> lista.usunOsobe(uuid, cb));
        }
    }

    private static void main(Runnable akcja) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(akcja);
    }

    private static void czekaj(BooleanSupplier warunek) {
        long koniec = SystemClock.uptimeMillis() + 15000;
        while (SystemClock.uptimeMillis() < koniec) {
            if (warunek.getAsBoolean()) return;
            SystemClock.sleep(25);
        }
        fail("Przekroczono czas oczekiwania na stan interfejsu");
    }

    private static void fragment(ActivityScenario<MainActivity> scenario, Class<?> typ) {
        czekaj(() -> {
            AtomicBoolean gotowe = new AtomicBoolean();
            scenario.onActivity(a -> {
                Fragment f = a.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                gotowe.set(typ.isInstance(f) && f.isResumed());
            });
            return gotowe.get();
        });
    }

    private static DodajOsobaViewModel model(ActivityScenario<MainActivity> scenario) {
        AtomicReference<DodajOsobaViewModel> wynik = new AtomicReference<>();
        scenario.onActivity(a -> wynik.set(new ViewModelProvider(
                a.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer))
                .get(DodajOsobaViewModel.class)));
        return wynik.get();
    }

    private static void gotowyFormularz(ActivityScenario<MainActivity> scenario) {
        fragment(scenario, DodajOsobaFragment.class);
        czekaj(() -> {
            AtomicBoolean gotowe = new AtomicBoolean();
            scenario.onActivity(a -> gotowe.set(new ViewModelProvider(
                    a.getSupportFragmentManager().findFragmentById(R.id.fragmentContainer))
                    .get(DodajOsobaViewModel.class).moznaEdytowac()));
            return gotowe.get();
        });
    }

    private static void kliknij(int id) {
        onView(withId(id)).perform(scrollTo(), click());
    }

    @Test public void zapisujeFormularzZAdresemPoObrocieIOdswiezaListePoEdycjiIUsunieciu() throws Exception {
        String imie = "Room" + UUID.randomUUID().toString().substring(0, 8);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnDodaj)).perform(click());
            gotowyFormularz(scenario);
            String uuid = model(scenario).uuid();
            testoweUuid.add(uuid);
            onView(withId(R.id.edtImie)).perform(replaceText(imie), closeSoftKeyboard());
            onView(withId(R.id.edtNazwisko)).perform(replaceText("Kowalski"), closeSoftKeyboard());
            kliknij(R.id.btnDodajAdres);
            onView(withId(R.id.edtMiasto)).perform(replaceText("Krakow"), closeSoftKeyboard());
            onView(withId(R.id.edtUlica)).perform(replaceText("Prosta"), closeSoftKeyboard());
            onView(withId(R.id.edtNumerDomu)).perform(replaceText("8A"), closeSoftKeyboard());
            onView(withId(R.id.edtKodPocztowy)).perform(replaceText("30-001"), closeSoftKeyboard());
            kliknij(R.id.btnZapiszAdres);
            gotowyFormularz(scenario);
            String adresUuid = model(scenario).adres().getIdAdres();
            scenario.recreate();
            gotowyFormularz(scenario);
            onView(withId(R.id.edtImie)).check(matches(withText(imie)));
            onView(withId(R.id.edtNazwisko)).check(matches(withText("Kowalski")));
            assertEquals(adresUuid, model(scenario).adres().getIdAdres());
            kliknij(R.id.btnDodajOsobe);
            fragment(scenario, ListaOsobFragment.class);
            czekajNaOsobeNaLiscie(scenario, uuid);
            Osoba zapisana = odczyt(cb -> lista.pobierzOsobe(uuid, cb));
            assertEquals(adresUuid, zapisana.getAdres().getIdAdres());
            onData(hasToString(containsString(imie))).inAdapterView(withId(R.id.listViewOsoby)).perform(click());
            gotowyFormularz(scenario);
            onView(withId(R.id.edtNazwisko)).perform(replaceText("Nowak"), closeSoftKeyboard());
            kliknij(R.id.btnDodajOsobe);
            fragment(scenario, ListaOsobFragment.class);
            czekajNaOsobeNaLiscie(scenario, uuid);
            onData(hasToString(imie + " Nowak")).inAdapterView(withId(R.id.listViewOsoby)).perform(click());
            gotowyFormularz(scenario);
            kliknij(R.id.btnUsunOsobe);
            onView(withId(android.R.id.button1)).perform(click());
            fragment(scenario, ListaOsobFragment.class);
            Osoba usunieta = odczyt(cb -> lista.pobierzOsobe(uuid, cb));
            assertNull(usunieta);
        }
    }

    private static void czekajNaOsobeNaLiscie(ActivityScenario<MainActivity> scenario, String uuid) {
        czekaj(() -> {
            AtomicBoolean znaleziona = new AtomicBoolean();
            scenario.onActivity(a -> {
                ListView lista = a.findViewById(R.id.listViewOsoby);
                for (int i = 0; i < lista.getAdapter().getCount(); i++) {
                    Osoba osoba = (Osoba) lista.getAdapter().getItem(i);
                    if (uuid.equals(osoba.getmIdOsoba())) znaleziona.set(true);
                }
            });
            return znaleziona.get();
        });
    }

    @Test public void anulowanieEdycjiOsobyNieZapisujeRoboczychZmianAdresu() throws Exception {
        Osoba osoba = new Osoba();
        osoba.setImie("Anulowanie" + UUID.randomUUID());
        osoba.setNazwisko("Test");
        osoba.setAdres(new Adres("Lodz", "Dluga", "1", "90-001"));
        testoweUuid.add(osoba.getmIdOsoba());
        zapis(cb -> lista.dodajOsobe(osoba, cb));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            czekajNaOsobeNaLiscie(scenario, osoba.getmIdOsoba());
            onData(hasToString(osoba.toString())).inAdapterView(withId(R.id.listViewOsoby)).perform(click());
            gotowyFormularz(scenario);
            kliknij(R.id.btnDodajAdres);
            onView(withId(R.id.edtMiasto)).perform(replaceText("Robocze"), closeSoftKeyboard());
            kliknij(R.id.btnZapiszAdres);
            gotowyFormularz(scenario);
            assertEquals(osoba.getAdres().getIdAdres(), model(scenario).adres().getIdAdres());
            kliknij(R.id.btnWroc);
            fragment(scenario, ListaOsobFragment.class);
            Osoba wynik = odczyt(cb -> lista.pobierzOsobe(osoba.getmIdOsoba(), cb));
            assertEquals("Lodz", wynik.getAdres().getMiasto());
        }
    }

    @Test public void obrotPodczasZapisuNiePowielaOperacjiINawigujeDopieroPoSukcesie() {
        try (KontrolowanaLista kontrolowana = new KontrolowanaLista();
             ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnDodaj)).perform(click());
            gotowyFormularz(scenario);
            DodajOsobaViewModel model = model(scenario);
            main(() -> model.inicjuj(kontrolowana, null));
            onView(withId(R.id.edtImie)).perform(replaceText("Oczekujacy"), closeSoftKeyboard());
            scenario.onActivity(a -> {
                a.findViewById(R.id.btnDodajOsobe).performClick();
                a.findViewById(R.id.btnDodajOsobe).performClick();
            });
            assertEquals(1, kontrolowana.zapisy);
            scenario.recreate();
            fragment(scenario, DodajOsobaFragment.class);
            assertSame(model, model(scenario));
            onView(withId(R.id.edtImie)).check(matches(withText("Oczekujacy")));
            onView(withId(R.id.btnDodajOsobe)).check(matches(not(isEnabled())));
            main(() -> kontrolowana.zapis.onSuccess(null));
            fragment(scenario, ListaOsobFragment.class);
            assertEquals(1, kontrolowana.zapisy);
        }
    }

    @Test public void obrotPodczasOdczytuZachowujeOczekiwanieINastepnieUzupelniaFormularz() throws Exception {
        Osoba osoba = new Osoba();
        osoba.setImie("Odczyt" + UUID.randomUUID().toString().substring(0, 8));
        osoba.setNazwisko("Po obrocie");
        testoweUuid.add(osoba.getmIdOsoba());
        zapis(cb -> lista.dodajOsobe(osoba, cb));
        CountDownLatch zablokowano = new CountDownLatch(1);
        CountDownLatch odblokuj = new CountDownLatch(1);
        ExecutorService blokada = Executors.newSingleThreadExecutor();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            czekajNaOsobeNaLiscie(scenario, osoba.getmIdOsoba());
            blokada.submit(() -> AppDatabase.getInstance(ApplicationProvider.getApplicationContext())
                    .runInTransaction(() -> {
                        zablokowano.countDown();
                        try { odblokuj.await(15, TimeUnit.SECONDS); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }));
            assertTrue(zablokowano.await(5, TimeUnit.SECONDS));
            java.util.concurrent.CompletableFuture<Void> oczekujacyZapis = new java.util.concurrent.CompletableFuture<>();
            lista.zmienOsobe(osoba, new ListaOsob.Callback<Void>() {
                @Override public void onSuccess(Void wynik) { oczekujacyZapis.complete(null); }
                @Override public void onError(Exception blad) { oczekujacyZapis.completeExceptionally(blad); }
            });
            onData(hasToString(osoba.toString())).inAdapterView(withId(R.id.listViewOsoby)).perform(click());
            fragment(scenario, DodajOsobaFragment.class);
            DodajOsobaViewModel przed = model(scenario);
            onView(withId(R.id.edtImie)).check(matches(not(isEnabled())));
            scenario.recreate();
            fragment(scenario, DodajOsobaFragment.class);
            assertSame(przed, model(scenario));
            onView(withId(R.id.btnDodajOsobe)).check(matches(not(isEnabled())));
            odblokuj.countDown();
            oczekujacyZapis.get(10, TimeUnit.SECONDS);
            gotowyFormularz(scenario);
            onView(withId(R.id.edtImie)).check(matches(withText(osoba.getImie())));
            onView(withId(R.id.edtNazwisko)).check(matches(withText("Po obrocie")));
        } finally {
            odblokuj.countDown();
            blokada.shutdown();
            assertTrue(blokada.awaitTermination(15, TimeUnit.SECONDS));
        }
    }

    @Test public void bladZapisuPozostawiaFormularzASpoznionyCallbackPoWyjsciuNieNawiguje() {
        try (KontrolowanaLista kontrolowana = new KontrolowanaLista();
             ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnDodaj)).perform(click());
            gotowyFormularz(scenario);
            DodajOsobaViewModel model = model(scenario);
            main(() -> model.inicjuj(kontrolowana, null));
            onView(withId(R.id.edtImie)).perform(replaceText("Zachowane"), closeSoftKeyboard());
            kliknij(R.id.btnDodajOsobe);
            main(() -> kontrolowana.zapis.onError(new IllegalStateException("Kontrolowany błąd")));
            gotowyFormularz(scenario);
            onView(withId(R.id.edtImie)).check(matches(withText("Zachowane")));
            kliknij(R.id.btnDodajOsobe);
            scenario.onActivity(a -> a.getSupportFragmentManager().popBackStackImmediate());
            fragment(scenario, ListaOsobFragment.class);
            onView(withId(R.id.btnDodaj)).perform(click());
            gotowyFormularz(scenario);
            String nowyUuid = model(scenario).uuid();
            main(() -> kontrolowana.zapis.onSuccess(null));
            fragment(scenario, DodajOsobaFragment.class);
            assertEquals(nowyUuid, model(scenario).uuid());
        }
    }

    @Test public void filtrIOdwrotneSortowanieDzialajaPoAsynchronicznymOdczycie() throws Exception {
        String prefiks = "Filtr" + UUID.randomUUID().toString().substring(0, 8);
        Osoba a = new Osoba();
        a.setImie(prefiks);
        a.setNazwisko("Adamski");
        Osoba z = new Osoba();
        z.setImie(prefiks);
        z.setNazwisko("Zielinski");
        for (Osoba osoba : new Osoba[]{a, z}) {
            testoweUuid.add(osoba.getmIdOsoba());
            zapis(cb -> lista.dodajOsobe(osoba, cb));
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnFiltruj)).perform(click());
            onView(withId(R.id.edtSzukaj)).perform(replaceText(prefiks.toUpperCase()), closeSoftKeyboard());
            onView(withId(R.id.rbMalejaco)).perform(click());
            onView(withId(R.id.btnZastosuj)).perform(click());
            fragment(scenario, ListaOsobFragment.class);
            czekajNaOsobeNaLiscie(scenario, z.getmIdOsoba());
            scenario.onActivity(activity -> {
                ListView widok = activity.findViewById(R.id.listViewOsoby);
                assertEquals(2, widok.getAdapter().getCount());
                assertEquals(z.getmIdOsoba(), ((Osoba) widok.getAdapter().getItem(0)).getmIdOsoba());
            });
            scenario.recreate();
            fragment(scenario, ListaOsobFragment.class);
            czekajNaOsobeNaLiscie(scenario, z.getmIdOsoba());
            scenario.onActivity(activity -> {
                ListView widok = activity.findViewById(R.id.listViewOsoby);
                assertEquals(2, widok.getAdapter().getCount());
                assertEquals(z.getmIdOsoba(), ((Osoba) widok.getAdapter().getItem(0)).getmIdOsoba());
            });
        }
    }
}
