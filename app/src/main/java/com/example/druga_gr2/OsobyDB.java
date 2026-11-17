package com.example.druga_gr2;

public class OsobyDB {
    public static final class OsobyTabela{
        public static final String Name = "Osoby";
        public static final class Kolumny{
            public static final String UUID = "OsobaUuid";
            public static final String IMIE = "Imie";
            public static final String NAZWISKO = "Nazwisko";
            public static final String ADRES_UUID = "AdresUuid";
        }
    }

    public static final class AdresTabela {
        public static final String Name = "Adres";

        public static final class Kolumny {
            public static final String UUID = "AdresUuid";
            public static final String MIASTO = "Miasto";
            public static final String ULICA = "Ulica";
            public static final String NUMER_DOMU = "NumerDomu";
            public static final String KOD_POCZTOWY = "KodPocztowy";
        }
    }
}
