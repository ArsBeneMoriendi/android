package com.example.druga_gr2;

import java.util.UUID;

public class Osoba {
    private String imie;
    private String nazwisko;
    private Adres adres;
    private String mIdOsoba;

    public Osoba(){
        mIdOsoba = UUID.randomUUID().toString();
        imie = "brak";
        nazwisko = "brak";
    }

    public String getImie() {
        return imie;
    }

    public void setImie(String imie) {
        this.imie = imie;
    }

    public String getNazwisko() {
        return nazwisko;
    }

    public void setNazwisko(String nazwisko) {
        this.nazwisko = nazwisko;
    }

    public Adres getAdres() {
        return adres;
    }

    public void setAdres(Adres adres) {
        this.adres = adres;
    }

    public String getmIdOsoba() {
        return mIdOsoba;
    }

    public void setmIdOsoba(String id) {
        mIdOsoba = id;
    }
    @Override
    public String toString(){
        return imie+" "+nazwisko;
    }
}

