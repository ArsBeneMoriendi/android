package com.example.druga_gr2;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import java.util.UUID;

public class DodajOsobaViewModel extends ViewModel {
    private final SavedStateHandle stan;
    private final MutableLiveData<Integer> zmiany = new MutableLiveData<>(0);
    private ListaOsob lista;
    private boolean wczytywanie;
    private boolean zapisywanie;
    private int komunikat;

    public DodajOsobaViewModel(SavedStateHandle stan) {
        this.stan = stan;
    }

    void inicjuj(ListaOsob lista, String uuidEdycji) {
        this.lista = lista;
        if (!stan.contains("uuid")) {
            stan.set("uuid", uuidEdycji == null ? UUID.randomUUID().toString() : uuidEdycji);
            stan.set("edycja", uuidEdycji != null);
            stan.set("imie", "");
            stan.set("nazwisko", "");
            stan.set("gotowe", uuidEdycji == null);
        }
        if (!gotowe() && !wczytywanie && !ukonczono()) wczytaj();
    }

    LiveData<Integer> zmiany() { return zmiany; }
    String uuid() { return stan.get("uuid"); }
    String imie() { return stan.get("imie"); }
    String nazwisko() { return stan.get("nazwisko"); }
    Adres adres() { return stan.get("adres"); }
    boolean edycja() { return Boolean.TRUE.equals(stan.get("edycja")); }
    boolean gotowe() { return Boolean.TRUE.equals(stan.get("gotowe")); }
    boolean ukonczono() { return Boolean.TRUE.equals(stan.get("ukonczono")); }
    boolean zajety() { return wczytywanie || zapisywanie || ukonczono(); }
    boolean moznaEdytowac() { return gotowe() && !zajety(); }

    void ustawPola(String imie, String nazwisko) {
        if (!moznaEdytowac()) return;
        stan.set("imie", imie);
        stan.set("nazwisko", nazwisko);
    }

    void ustawAdres(Adres adres) {
        if (!moznaEdytowac()) return;
        stan.set("adres", ListaOsob.kopiujAdres(adres));
        powiadom();
    }

    void wczytaj() {
        if (wczytywanie || gotowe()) return;
        wczytywanie = true;
        powiadom();
        lista.pobierzOsobe(uuid(), new ListaOsob.Callback<Osoba>() {
            @Override public void onSuccess(Osoba osoba) {
                wczytywanie = false;
                if (osoba == null) {
                    komunikat = R.string.osoba_nie_istnieje;
                } else if (!gotowe()) {
                    stan.set("imie", osoba.getImie());
                    stan.set("nazwisko", osoba.getNazwisko());
                    stan.set("adres", osoba.getAdres());
                    stan.set("gotowe", true);
                }
                powiadom();
            }
            @Override public void onError(Exception blad) {
                wczytywanie = false;
                blad(blad, R.string.blad_odczytu);
            }
        });
    }

    void zapisz() {
        if (!moznaEdytowac()) return;
        Osoba osoba = new Osoba();
        osoba.setmIdOsoba(uuid());
        osoba.setImie(imie());
        osoba.setNazwisko(nazwisko());
        osoba.setAdres(adres());
        zapisywanie = true;
        powiadom();
        if (edycja()) lista.zmienOsobe(osoba, wynikZapisu());
        else lista.dodajOsobe(osoba, wynikZapisu());
    }

    void usun() {
        if (!moznaEdytowac() || !edycja()) return;
        zapisywanie = true;
        powiadom();
        lista.usunOsobe(uuid(), wynikZapisu());
    }

    private ListaOsob.Callback<Void> wynikZapisu() {
        return new ListaOsob.Callback<Void>() {
            @Override public void onSuccess(Void wynik) {
                zapisywanie = false;
                stan.set("ukonczono", true);
                powiadom();
            }
            @Override public void onError(Exception blad) {
                zapisywanie = false;
                blad(blad, R.string.blad_zapisu);
            }
        };
    }

    int odbierzKomunikat() {
        int wynik = komunikat;
        komunikat = 0;
        return wynik;
    }

    private void blad(Exception blad, int tekst) {
        Log.e("DodajOsoba", "Operacja na bazie nie powiodła się", blad);
        komunikat = tekst;
        powiadom();
    }

    private void powiadom() { zmiany.setValue(zmiany.getValue() + 1); }
}
