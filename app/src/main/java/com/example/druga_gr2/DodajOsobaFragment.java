package com.example.druga_gr2;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.Fragment;

import java.util.UUID;

public class DodajOsobaFragment extends Fragment {
    public static final String ARG_UUID_OSOBY = "uuid_osoby";
    private static final String STAN_UUID = "uuid_formularza";
    private static final String STAN_IMIE = "imie_formularza";
    private static final String STAN_NAZWISKO = "nazwisko_formularza";

    private Button btnWroc;
    private Button btnDodajOsobe;
    private Button btnDodajAdres;
    private Button btnUsunOsobe;
    private EditText edtImie;
    private EditText edtNazwisko;
    private TextView txtAdres;
    private ListaOsob listaOsob;
    private Adres adres;
    private String uuid;
    private String imie = "";
    private String nazwisko = "";
    private boolean edycja;
    private int bladWczytywania;
    private ActivityResultLauncher<Intent> adresLauncher;

    public DodajOsobaFragment() {
    }

    public static DodajOsobaFragment newInstance(String param1, String param2) {
        return new DodajOsobaFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        edycja = getArguments() != null && getArguments().containsKey(ARG_UUID_OSOBY);
        uuid = edycja ? getArguments().getString(ARG_UUID_OSOBY) : UUID.randomUUID().toString();

        try {
            listaOsob = ListaOsob.get(requireContext());
            if (savedInstanceState != null) {
                savedInstanceState.setClassLoader(Adres.class.getClassLoader());
                uuid = savedInstanceState.getString(STAN_UUID);
                imie = savedInstanceState.getString(STAN_IMIE, "");
                nazwisko = savedInstanceState.getString(STAN_NAZWISKO, "");
                adres = BundleCompat.getParcelable(savedInstanceState, AdresActivity.EXTRA_ADRES, Adres.class);
            } else if (edycja) {
                Osoba osoba = listaOsob.pobierzOsobe(uuid);
                if (osoba == null) {
                    bladWczytywania = R.string.osoba_nie_istnieje;
                } else {
                    imie = osoba.getImie();
                    nazwisko = osoba.getNazwisko();
                    adres = osoba.getAdres();
                }
            }
        } catch (SQLiteException | IllegalArgumentException e) {
            Log.e("DodajOsobaFragment", "Nie udało się wczytać osoby", e);
            bladWczytywania = R.string.blad_odczytu;
        }

        adresLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent dane = result.getData();
                        dane.setExtrasClassLoader(Adres.class.getClassLoader());
                        if (dane.getBooleanExtra(AdresActivity.EXTRA_USUN_ADRES, false)) {
                            adres = null;
                        } else {
                            Adres nowyAdres = IntentCompat.getParcelableExtra(
                                    dane, AdresActivity.EXTRA_ADRES, Adres.class);
                            if (nowyAdres != null) {
                                adres = nowyAdres;
                            }
                        }
                        wyswietlAdres();
                    }
                });
    }

    private void zapamietajPola() {
        if (edtImie != null) {
            imie = edtImie.getText().toString();
            nazwisko = edtNazwisko.getText().toString();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        zapamietajPola();
        outState.putString(STAN_UUID, uuid);
        outState.putString(STAN_IMIE, imie);
        outState.putString(STAN_NAZWISKO, nazwisko);
        outState.putParcelable(AdresActivity.EXTRA_ADRES, adres);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dodaj_osoba, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnDodajOsobe = view.findViewById(R.id.btnDodajOsobe);
        btnWroc = view.findViewById(R.id.btnWroc);
        btnDodajAdres = view.findViewById(R.id.btnDodajAdres);
        btnUsunOsobe = view.findViewById(R.id.btnUsunOsobe);
        edtImie = view.findViewById(R.id.edtImie);
        edtNazwisko = view.findViewById(R.id.edtNazwisko);
        txtAdres = view.findViewById(R.id.txtAdres);
        edtImie.setText(imie);
        edtNazwisko.setText(nazwisko);
        btnDodajOsobe.setText(edycja ? R.string.zapisz_zmiany : R.string.dodaj_osobe);
        btnUsunOsobe.setVisibility(edycja ? View.VISIBLE : View.GONE);
        wyswietlAdres();
        przyciski();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bladWczytywania != 0) {
            Toast.makeText(requireContext(), bladWczytywania, Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
            return;
        }
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(edycja ? R.string.edytuj_osobe : R.string.dodaj_osobe);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        zapamietajPola();
        super.onDestroyView();
        btnWroc = null;
        btnDodajOsobe = null;
        btnDodajAdres = null;
        btnUsunOsobe = null;
        edtImie = null;
        edtNazwisko = null;
        txtAdres = null;
    }

    private void wyswietlAdres() {
        if (txtAdres == null) {
            return;
        }
        btnDodajAdres.setText(adres == null ? R.string.dodaj_adres : R.string.edytuj_adres);
        if (adres == null) {
            txtAdres.setText(R.string.brak_adresu);
        } else {
            txtAdres.setText(getString(R.string.podsumowanie_adresu,
                    adres.getUlica(), adres.getNumerDomu(),
                    adres.getKodPocztowy(), adres.getMiasto()));
        }
    }

    private void przyciski() {
        btnDodajAdres.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), AdresActivity.class);
            if (adres != null) {
                intent.putExtra(AdresActivity.EXTRA_ADRES, adres);
            }
            adresLauncher.launch(intent);
        });
        btnWroc.setOnClickListener(view -> getParentFragmentManager().popBackStack());
        btnDodajOsobe.setOnClickListener(view -> zapiszOsobe());
        btnUsunOsobe.setOnClickListener(view ->
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.usun_osobe)
                        .setMessage(R.string.potwierdz_usuniecie_osoby)
                        .setNegativeButton(R.string.anuluj, null)
                        .setPositiveButton(R.string.usun_osobe, (dialog, which) -> {
                            try {
                                listaOsob.usunOsobe(uuid);
                                getParentFragmentManager().popBackStack();
                            } catch (SQLiteException | IllegalArgumentException e) {
                                pokazBladZapisu(e);
                            }
                        }).show());
    }

    private void zapiszOsobe() {
        zapamietajPola();
        Osoba osoba = new Osoba();
        osoba.setmIdOsoba(uuid);
        osoba.setImie(imie);
        osoba.setNazwisko(nazwisko);
        osoba.setAdres(adres);
        btnDodajOsobe.setEnabled(false);
        try {
            if (edycja) {
                listaOsob.zmienOsobe(osoba);
            } else {
                listaOsob.dodajOsobe(osoba);
            }
            getParentFragmentManager().popBackStack();
        } catch (SQLiteException | IllegalArgumentException e) {
            btnDodajOsobe.setEnabled(true);
            pokazBladZapisu(e);
        }
    }

    private void pokazBladZapisu(Exception e) {
        Log.e("DodajOsobaFragment", "Nie udało się zapisać zmian", e);
        Toast.makeText(requireContext(), R.string.blad_zapisu, Toast.LENGTH_LONG).show();
    }
}
