package com.example.druga_gr2;

import android.os.Handler;
import android.os.Looper;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import com.example.druga_gr2.database.AppDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class KontrolowanaLista extends ListaOsob implements AutoCloseable {
    private final AppDatabase baza;
    private final ExecutorService executor;
    Callback<Osoba> odczyt;
    Callback<Void> zapis;
    Osoba zapisana;
    int odczyty;
    int zapisy;

    KontrolowanaLista() {
        this(Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase.class)
                .build(), Executors.newSingleThreadExecutor());
    }

    private KontrolowanaLista(AppDatabase baza, ExecutorService executor) {
        super(baza, executor, new Handler(Looper.getMainLooper()));
        this.baza = baza;
        this.executor = executor;
    }

    @Override public void pobierzOsobe(String uuid, Callback<Osoba> callback) {
        odczyty++;
        odczyt = callback;
    }

    @Override public void dodajOsobe(Osoba osoba, Callback<Void> callback) {
        zapisy++;
        zapisana = osoba;
        zapis = callback;
    }

    @Override public void zmienOsobe(Osoba osoba, Callback<Void> callback) {
        dodajOsobe(osoba, callback);
    }

    @Override public void usunOsobe(String uuid, Callback<Void> callback) {
        zapisy++;
        zapis = callback;
    }

    @Override public void close() {
        executor.shutdownNow();
        baza.close();
    }
}
