package com.example.druga_gr2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.druga_gr2.database.AdresEntity;
import com.example.druga_gr2.database.AppDatabase;
import com.example.druga_gr2.database.OsobaEntity;
import com.example.druga_gr2.database.OsobaZAdresem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListaOsob {
    public interface Callback<T> {
        void onSuccess(T wynik);
        void onError(Exception blad);
    }

    private static ListaOsob instance;
    private final AppDatabase database;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private ListaOsob(Context context) {
        this(AppDatabase.getInstance(context), Executors.newSingleThreadExecutor(),
                new Handler(Looper.getMainLooper()));
    }

    ListaOsob(AppDatabase database, ExecutorService executor, Handler mainHandler) {
        this.database = database;
        this.executor = executor;
        this.mainHandler = mainHandler;
    }

    public static synchronized ListaOsob get(Context context) {
        if (instance == null) instance = new ListaOsob(context.getApplicationContext());
        return instance;
    }

    public void pobierzOsoby(Callback<List<Osoba>> callback) {
        wykonaj(() -> {
            List<Osoba> wynik = new ArrayList<>();
            for (OsobaZAdresem relacja : database.osobaDao().pobierzOsobyZAdresami()) {
                wynik.add(model(relacja));
            }
            return wynik;
        }, callback);
    }

    public void pobierzOsobe(String uuid, Callback<Osoba> callback) {
        wykonaj(() -> {
            sprawdzUuid(uuid);
            OsobaZAdresem relacja = database.osobaDao().pobierzOsobeZAdresem(uuid);
            return relacja == null ? null : model(relacja);
        }, callback);
    }

    public void dodajOsobe(Osoba osoba, Callback<Void> callback) {
        Osoba kopia = kopiuj(osoba);
        wykonaj(() -> {
            OsobaEntity encja = encja(kopia);
            database.runInTransaction(() -> {
                zapiszAdres(kopia.getAdres());
                database.osobaDao().dodajOsobe(encja);
            });
            return null;
        }, callback);
    }

    public void zmienOsobe(Osoba osoba, Callback<Void> callback) {
        Osoba kopia = kopiuj(osoba);
        wykonaj(() -> {
            OsobaEntity encja = encja(kopia);
            database.runInTransaction(() -> {
                OsobaEntity stara = wymaganaOsoba(encja.uuid);
                encja.id = stara.id;
                zapiszAdres(kopia.getAdres());
                wymaganyJeden(database.osobaDao().aktualizujOsobe(encja));
                if (stara.adresUuid != null && !Objects.equals(stara.adresUuid, encja.adresUuid)) {
                    database.adresDao().usunJesliNieuzywany(stara.adresUuid);
                }
            });
            return null;
        }, callback);
    }

    public void usunOsobe(String uuid, Callback<Void> callback) {
        wykonaj(() -> {
            sprawdzUuid(uuid);
            database.runInTransaction(() -> {
                OsobaEntity osoba = wymaganaOsoba(uuid);
                wymaganyJeden(database.osobaDao().usunOsobe(osoba));
                if (osoba.adresUuid != null) database.adresDao().usunJesliNieuzywany(osoba.adresUuid);
            });
            return null;
        }, callback);
    }

    public void zmienAdres(Adres adres, Callback<Void> callback) {
        Adres kopia = kopiujAdres(adres);
        wykonaj(() -> {
            wymaganyJeden(database.adresDao().aktualizujAdres(encjaAdresu(kopia)));
            return null;
        }, callback);
    }

    public void usunAdres(String uuid, Callback<Void> callback) {
        wykonaj(() -> {
            sprawdzUuid(uuid);
            database.runInTransaction(() -> {
                AdresEntity adres = database.adresDao().pobierzAdres(uuid);
                if (adres == null) throw new IllegalStateException("Adres nie istnieje");
                wymaganyJeden(database.adresDao().usunAdres(adres));
            });
            return null;
        }, callback);
    }

    private <T> void wykonaj(Callable<T> operacja, Callback<T> callback) {
        executor.execute(() -> {
            final T wynik;
            try {
                wynik = operacja.call();
            } catch (Exception blad) {
                mainHandler.post(() -> callback.onError(blad));
                return;
            }
            mainHandler.post(() -> callback.onSuccess(wynik));
        });
    }

    private void zapiszAdres(Adres adres) {
        if (adres == null) return;
        AdresEntity encja = encjaAdresu(adres);
        if (database.adresDao().pobierzAdres(encja.uuid) == null) {
            database.adresDao().dodajAdres(encja);
        } else {
            wymaganyJeden(database.adresDao().aktualizujAdres(encja));
        }
    }

    private OsobaEntity wymaganaOsoba(String uuid) {
        OsobaEntity osoba = database.osobaDao().pobierzOsobe(uuid);
        if (osoba == null) throw new IllegalStateException("Osoba nie istnieje");
        return osoba;
    }

    private static void wymaganyJeden(int count) {
        if (count != 1) throw new IllegalStateException("Oczekiwano zmiany jednego rekordu: " + count);
    }

    private static void sprawdzUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) throw new IllegalArgumentException("UUID nie może być pusty");
    }

    private static OsobaEntity encja(Osoba osoba) {
        if (osoba == null) throw new IllegalArgumentException("Brak osoby");
        sprawdzUuid(osoba.getmIdOsoba());
        OsobaEntity encja = new OsobaEntity();
        encja.uuid = osoba.getmIdOsoba();
        encja.imie = osoba.getImie();
        encja.nazwisko = osoba.getNazwisko();
        if (osoba.getAdres() != null) {
            sprawdzUuid(osoba.getAdres().getIdAdres());
            encja.adresUuid = osoba.getAdres().getIdAdres();
        }
        return encja;
    }

    private static AdresEntity encjaAdresu(Adres adres) {
        if (adres == null) throw new IllegalArgumentException("Brak adresu");
        sprawdzUuid(adres.getIdAdres());
        AdresEntity encja = new AdresEntity();
        encja.uuid = adres.getIdAdres();
        encja.miasto = adres.getMiasto();
        encja.ulica = adres.getUlica();
        encja.numerDomu = adres.getNumerDomu();
        encja.kodPocztowy = adres.getKodPocztowy();
        return encja;
    }

    private static Osoba model(OsobaZAdresem relacja) {
        Osoba osoba = new Osoba();
        osoba.setmIdOsoba(relacja.osoba.uuid);
        osoba.setImie(relacja.osoba.imie);
        osoba.setNazwisko(relacja.osoba.nazwisko);
        if (relacja.adres != null) {
            AdresEntity encja = relacja.adres;
            Adres adres = new Adres(encja.miasto, encja.ulica, encja.numerDomu, encja.kodPocztowy);
            adres.setIdAdres(encja.uuid);
            osoba.setAdres(adres);
        }
        return osoba;
    }

    private static Osoba kopiuj(Osoba zrodlo) {
        if (zrodlo == null) return null;
        Osoba kopia = new Osoba();
        kopia.setmIdOsoba(zrodlo.getmIdOsoba());
        kopia.setImie(zrodlo.getImie());
        kopia.setNazwisko(zrodlo.getNazwisko());
        kopia.setAdres(kopiujAdres(zrodlo.getAdres()));
        return kopia;
    }

    static Adres kopiujAdres(Adres zrodlo) {
        if (zrodlo == null) return null;
        Adres kopia = new Adres(zrodlo.getMiasto(), zrodlo.getUlica(),
                zrodlo.getNumerDomu(), zrodlo.getKodPocztowy());
        kopia.setIdAdres(zrodlo.getIdAdres());
        return kopia;
    }
}
