package com.example.druga_gr2;

import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;


public class ListaOsobFragment extends Fragment {

    private ListView lvListaOsob;
    private Button btnDodaj;
    private Button btnFiltruj;
    private ArrayAdapter<Osoba> adapterOsob;
    private ListaOsob listaOsob;
    private ParametryFiltrowania parametryFiltrowania;
    private static final String STAN_FILTRA = "stan_filtra";

    public ListaOsobFragment() {
        // Required empty public constructor
    }

    public static ListaOsobFragment newInstance(String param1, String param2) {
        ListaOsobFragment fragment = new ListaOsobFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            savedInstanceState.setClassLoader(ParametryFiltrowania.class.getClassLoader());
            parametryFiltrowania = BundleCompat.getParcelable(
                    savedInstanceState, STAN_FILTRA, ParametryFiltrowania.class);
        }
        if (parametryFiltrowania == null) {
            parametryFiltrowania = new ParametryFiltrowania();
        }
        getParentFragmentManager().setFragmentResultListener(FiltrFragment.ODPOWIEDZ_FILTRA, this, ((requestKey, result) ->
        {
            result.setClassLoader(ParametryFiltrowania.class.getClassLoader());
            ParametryFiltrowania noweParametry = BundleCompat.getParcelable(result, FiltrFragment.RESULT_PARAMETRY, ParametryFiltrowania.class);
            if(noweParametry != null){
                parametryFiltrowania = noweParametry;
                odswiezListe();
            }
        }));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(STAN_FILTRA, parametryFiltrowania);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lista_osob, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inicjuj(view);
        przyciski();
    }

    @Override
    public void onResume() {
        super.onResume();
        odswiezListe();

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar!=null){
            actionBar.setTitle("Lista osób");
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        adapterOsob = null;
        lvListaOsob = null;
        btnDodaj = null;
        btnFiltruj = null;
    }

    private void inicjuj(View widok){
        lvListaOsob = widok.findViewById(R.id.listViewOsoby);
        btnDodaj = widok.findViewById(R.id.btnDodaj);
        btnFiltruj = widok.findViewById(R.id.btnFiltruj);
        adapterOsob = new ArrayAdapter<Osoba>(requireContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        lvListaOsob.setAdapter(adapterOsob);
    }
    private void przyciski(){
        lvListaOsob.setOnItemClickListener((parent, view, position, id) -> {
            Osoba osoba = adapterOsob.getItem(position);
            if (osoba == null) {
                return;
            }
            Bundle args = new Bundle();
            args.putString(DodajOsobaFragment.ARG_UUID_OSOBY, osoba.getmIdOsoba());
            getParentFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, DodajOsobaFragment.class, args)
                    .addToBackStack(null)
                    .commit();
        });
        //Dodawanie nowej osoby
        btnDodaj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getParentFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragmentContainer,DodajOsobaFragment.class, null)
                        .addToBackStack(null)
                        .commit();
            }
        });
        //filtrowanie listy osób
        btnFiltruj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle args = new Bundle();
                args.putParcelable(FiltrFragment.ARG_PARAMETRY, parametryFiltrowania);
                getParentFragmentManager()
                        .beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragmentContainer, FiltrFragment.class, args)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }
    private void odswiezListe(){
        if (adapterOsob == null) {
            return;
        }
        ArrayList<Osoba> wynik = new ArrayList<>();
        String tekst = parametryFiltrowania.getTekst().trim().toLowerCase(Locale.ROOT);

        //filtrowanie
        try {
            listaOsob = ListaOsob.get(requireContext());
            for (Osoba osoba : listaOsob.pobierzOsoby()) {
                String imie = osoba.getImie() == null ? "" : osoba.getImie();
                String nazwisko = osoba.getNazwisko() == null ? "" : osoba.getNazwisko();
                String nazwa = (imie + " " + nazwisko).toLowerCase(Locale.ROOT);
                if (nazwa.contains(tekst)) {
                    wynik.add(osoba);
                }
            }
        } catch (SQLiteException e) {
            Log.e("ListaOsobFragment", "Nie udało się odczytać listy osób", e);
            Toast.makeText(requireContext(), R.string.blad_odczytu, Toast.LENGTH_LONG).show();
            return;
        }

        //Sortowanie według nazwiska
        Comparator<Osoba> comparator = Comparator.comparing(Osoba::getNazwisko,
                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
        if(!parametryFiltrowania.isSortowanieRosnaco()){
            comparator = comparator.reversed();
        }
        wynik.sort(comparator);

        adapterOsob.setNotifyOnChange(false);
        adapterOsob.clear();
        adapterOsob.addAll(wynik);
        adapterOsob.notifyDataSetChanged();
    }
}
