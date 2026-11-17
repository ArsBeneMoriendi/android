package com.example.druga_gr2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class FiltrowanieOsob {
    private FiltrowanieOsob() { }

    static List<Osoba> zastosuj(List<Osoba> osoby, String tekst, boolean rosnaco) {
        String szukany = tekst == null ? "" : tekst.trim().toLowerCase(Locale.ROOT);
        List<Osoba> wynik = new ArrayList<>();
        for (Osoba osoba : osoby) {
            String imie = osoba.getImie() == null ? "" : osoba.getImie();
            String nazwisko = osoba.getNazwisko() == null ? "" : osoba.getNazwisko();
            if ((imie + " " + nazwisko).toLowerCase(Locale.ROOT).contains(szukany)) wynik.add(osoba);
        }
        Comparator<Osoba> comparator = Comparator.comparing(Osoba::getNazwisko,
                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
        wynik.sort(rosnaco ? comparator : comparator.reversed());
        return wynik;
    }
}
