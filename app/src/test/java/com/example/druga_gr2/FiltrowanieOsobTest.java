package com.example.druga_gr2;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class FiltrowanieOsobTest {
    private Osoba osoba(String imie, String nazwisko) {
        Osoba osoba = new Osoba();
        osoba.setImie(imie);
        osoba.setNazwisko(nazwisko);
        return osoba;
    }

    @Test public void filtrujeImieNazwiskoOrazCalaNazweBezRozroznianiaLiter() {
        Osoba jan = osoba("Jan", "Kowalski");
        List<Osoba> lista = Arrays.asList(jan, osoba("Anna", "Nowak"));
        for (String tekst : Arrays.asList(" JAN ", "KOWAL", "jan kow")) {
            assertEquals(Arrays.asList(jan), FiltrowanieOsob.zastosuj(lista, tekst, true));
        }
        assertTrue(FiltrowanieOsob.zastosuj(lista, "brak", true).isEmpty());
    }

    @Test public void sortujeObaKierunkiBezZmianyWejscia() {
        Osoba z = osoba("Zofia", "Zielinska");
        Osoba a = osoba("Adam", "adamski");
        Osoba b = osoba("Beata", "Baran");
        List<Osoba> lista = Arrays.asList(z, a, b);
        assertEquals(Arrays.asList(a, b, z), FiltrowanieOsob.zastosuj(lista, "", true));
        assertEquals(Arrays.asList(z, b, a), FiltrowanieOsob.zastosuj(lista, "", false));
        assertEquals(Arrays.asList(z, a, b), lista);
    }

    @Test public void obslugujeNullOrazZachowujeKolejnoscRownychNazwisk() {
        Osoba brak = osoba(null, null);
        Osoba pierwsza = osoba("A", "Nowak");
        Osoba druga = osoba("B", "nowak");
        List<Osoba> lista = Arrays.asList(pierwsza, brak, druga);
        assertEquals(Arrays.asList(brak, pierwsza, druga), FiltrowanieOsob.zastosuj(lista, null, true));
        assertEquals(Arrays.asList(pierwsza, druga, brak), FiltrowanieOsob.zastosuj(lista, "", false));
    }
}
