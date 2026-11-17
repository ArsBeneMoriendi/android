package com.example.druga_gr2;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class FiltrFragment extends Fragment {

    public static final String ARG_PARAMETRY = "ARG_PARAMETRY";
    public static final String ODPOWIEDZ_FILTRA = "ODPOWIEDZ_FILTRA";
    public static final String RESULT_PARAMETRY = "RESULT_PARAMETRY";

    private EditText edtSzukaj;
    private RadioGroup rgSortowanie;
    private RadioButton rbMalejaco;
    private RadioButton rbRosnaco;
    private Button btnWroc;
    private Button btnZastosuj;

    public FiltrFragment() {
        // Required empty public constructor
    }


    public static FiltrFragment newInstance(String param1, String param2) {
        FiltrFragment fragment = new FiltrFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_filtr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        inicjuj(view);

        if(args != null){
            args.setClassLoader(ParametryFiltrowania.class.getClassLoader());
            ParametryFiltrowania parametry = BundleCompat.getParcelable(args, ARG_PARAMETRY, ParametryFiltrowania.class);
            if(parametry != null){
                edtSzukaj.setText(parametry.getTekst());
                rgSortowanie.check(parametry.isSortowanieRosnaco()? R.id.rbRosnaco:R.id.rbMalejaco);
            }
        }

        przyciski();
    }

    @Override
    public void onResume() {
        super.onResume();
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle("Filtrowanie");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void inicjuj(View widok){
        edtSzukaj = widok.findViewById(R.id.edtSzukaj);
        rgSortowanie = widok.findViewById(R.id.rgSortowanie);
        rbMalejaco = widok.findViewById(R.id.rbMalejaco);
        rbRosnaco = widok.findViewById(R.id.rbRosnaco);
        btnWroc = widok.findViewById(R.id.btnWrocFiltr);
        btnZastosuj = widok.findViewById(R.id.btnZastosuj);
    }
    private void przyciski(){
        btnWroc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnZastosuj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParametryFiltrowania noweParametry = new ParametryFiltrowania();

                noweParametry.setTekst(edtSzukaj.getText().toString().trim());
                noweParametry.setSortowanieRosnaco(rgSortowanie.getCheckedRadioButtonId() == R.id.rbRosnaco);

                Bundle wynik = new Bundle();
                wynik.putParcelable(RESULT_PARAMETRY, noweParametry);
                getParentFragmentManager().setFragmentResult(ODPOWIEDZ_FILTRA, wynik);
                getParentFragmentManager().popBackStack();
            }
        });
    }
}