package com.example.druga_gr2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class DodajOsobaFragment extends Fragment {
    public static final String ARG_UUID_OSOBY = "uuid_osoby";
    private DodajOsobaViewModel model;
    private ActivityResultLauncher<Intent> adresLauncher;
    private Button btnWroc, btnDodajOsobe, btnDodajAdres, btnUsunOsobe, btnPonow;
    private EditText edtImie, edtNazwisko;
    private TextView txtAdres;
    private boolean renderowanie;
    private boolean nawigacja;

    public DodajOsobaFragment() { }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = new ViewModelProvider(this).get(DodajOsobaViewModel.class);
        model.inicjuj(ListaOsob.get(requireContext()), getArguments() == null
                ? null : getArguments().getString(ARG_UUID_OSOBY));
        adresLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
            Intent dane = result.getData();
            dane.setExtrasClassLoader(Adres.class.getClassLoader());
            if (dane.getBooleanExtra(AdresActivity.EXTRA_USUN_ADRES, false)) {
                model.ustawAdres(null);
            } else {
                Adres adres = IntentCompat.getParcelableExtra(dane, AdresActivity.EXTRA_ADRES, Adres.class);
                if (adres != null) model.ustawAdres(adres);
            }
        });
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dodaj_osoba, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nawigacja = false;
        btnWroc = view.findViewById(R.id.btnWroc);
        btnDodajOsobe = view.findViewById(R.id.btnDodajOsobe);
        btnDodajAdres = view.findViewById(R.id.btnDodajAdres);
        btnUsunOsobe = view.findViewById(R.id.btnUsunOsobe);
        btnPonow = view.findViewById(R.id.btnPonowOdczyt);
        edtImie = view.findViewById(R.id.edtImie);
        edtNazwisko = view.findViewById(R.id.edtNazwisko);
        txtAdres = view.findViewById(R.id.txtAdres);
        edtImie.setSaveEnabled(false);
        edtNazwisko.setSaveEnabled(false);
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!renderowanie) zapamietajPola();
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        edtImie.addTextChangedListener(watcher);
        edtNazwisko.addTextChangedListener(watcher);
        btnWroc.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnPonow.setOnClickListener(v -> model.wczytaj());
        btnDodajOsobe.setOnClickListener(v -> {
            zapamietajPola();
            model.zapisz();
        });
        btnDodajAdres.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AdresActivity.class);
            if (model.adres() != null) intent.putExtra(AdresActivity.EXTRA_ADRES, model.adres());
            adresLauncher.launch(intent);
        });
        btnUsunOsobe.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle(R.string.usun_osobe)
                .setMessage(R.string.potwierdz_usuniecie_osoby)
                .setNegativeButton(R.string.anuluj, null)
                .setPositiveButton(R.string.usun_osobe, (dialog, which) -> {
                    if (isResumed() && getView() != null) model.usun();
                }).show());
        model.zmiany().observe(getViewLifecycleOwner(), ignored -> wyswietl());
        wyswietl();
    }

    @Override public void onResume() {
        super.onResume();
        ActionBar bar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (bar != null) {
            bar.setTitle(model.edycja() ? R.string.edytuj_osobe : R.string.dodaj_osobe);
            bar.setDisplayHomeAsUpEnabled(true);
        }
        obsluzWynik();
    }

    private void zapamietajPola() {
        if (edtImie != null) model.ustawPola(edtImie.getText().toString(), edtNazwisko.getText().toString());
    }

    private void wyswietl() {
        if (edtImie == null) return;
        renderowanie = true;
        ustawTekst(edtImie, model.imie());
        ustawTekst(edtNazwisko, model.nazwisko());
        renderowanie = false;
        boolean aktywne = model.moznaEdytowac();
        edtImie.setEnabled(aktywne);
        edtNazwisko.setEnabled(aktywne);
        btnDodajOsobe.setEnabled(aktywne);
        btnDodajAdres.setEnabled(aktywne);
        btnUsunOsobe.setEnabled(aktywne);
        btnWroc.setEnabled(!model.zajety());
        btnPonow.setVisibility(!model.gotowe() && !model.zajety() ? View.VISIBLE : View.GONE);
        btnDodajOsobe.setText(model.edycja() ? R.string.zapisz_zmiany : R.string.dodaj_osobe);
        btnUsunOsobe.setVisibility(model.edycja() ? View.VISIBLE : View.GONE);
        Adres adres = model.adres();
        btnDodajAdres.setText(adres == null ? R.string.dodaj_adres : R.string.edytuj_adres);
        if (adres == null) txtAdres.setText(R.string.brak_adresu);
        else txtAdres.setText(getString(R.string.podsumowanie_adresu, adres.getUlica(),
                adres.getNumerDomu(), adres.getKodPocztowy(), adres.getMiasto()));
        obsluzWynik();
    }

    private static void ustawTekst(EditText pole, String tekst) {
        String wartosc = tekst == null ? "" : tekst;
        if (!pole.getText().toString().equals(wartosc)) pole.setText(wartosc);
    }

    private void obsluzWynik() {
        if (getView() == null || !isResumed() || getParentFragmentManager().isStateSaved()
                || getParentFragmentManager().findFragmentById(R.id.fragmentContainer) != this) return;
        int komunikat = model.odbierzKomunikat();
        if (komunikat != 0) Toast.makeText(requireContext(), komunikat, Toast.LENGTH_LONG).show();
        if (model.ukonczono() && !nawigacja) {
            nawigacja = true;
            getParentFragmentManager().popBackStack();
        }
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        zapamietajPola();
        super.onSaveInstanceState(outState);
    }

    @Override public void onDestroyView() {
        zapamietajPola();
        super.onDestroyView();
        btnWroc = btnDodajOsobe = btnDodajAdres = btnUsunOsobe = btnPonow = null;
        edtImie = edtNazwisko = null;
        txtAdres = null;
    }
}
