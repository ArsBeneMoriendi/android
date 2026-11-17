package com.example.druga_gr2;

import android.os.Looper;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class RoomTestSupport {
    private RoomTestSupport() { }

    static <T> T odczyt(Consumer<ListaOsob.Callback<T>> operacja) throws Exception {
        CompletableFuture<T> wynik = new CompletableFuture<>();
        operacja.accept(new ListaOsob.Callback<T>() {
            @Override public void onSuccess(T wartosc) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wynik.completeExceptionally(new AssertionError("Callback poza głównym wątkiem"));
                } else wynik.complete(wartosc);
            }
            @Override public void onError(Exception blad) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wynik.completeExceptionally(new AssertionError("Błąd poza głównym wątkiem"));
                } else wynik.completeExceptionally(blad);
            }
        });
        return wynik.get(15, TimeUnit.SECONDS);
    }

    static void zapis(Consumer<ListaOsob.Callback<Void>> operacja) throws Exception {
        odczyt(operacja);
    }
}
