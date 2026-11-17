package com.example.druga_gr2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.UUID;

public class AdresActivity extends AppCompatActivity {
    public static final String EXTRA_ADRES = "ADRES";
    public static final String EXTRA_USUN_ADRES = "USUN_ADRES";
    private static final String STAN_UUID = "uuid_adresu";

    private EditText edtMiasto;
    private EditText edtUlica;
    private EditText edtNumerDomu;
    private EditText edtKodPocztowy;
    private String uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adres);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        edtMiasto = findViewById(R.id.edtMiasto);
        edtUlica = findViewById(R.id.edtUlica);
        edtNumerDomu = findViewById(R.id.edtNumerDomu);
        edtKodPocztowy = findViewById(R.id.edtKodPocztowy);
        Button btnWroc = findViewById(R.id.btnWrocAdres);
        Button btnZapiszAdres = findViewById(R.id.btnZapiszAdres);
        Button btnUsunAdres = findViewById(R.id.btnUsunAdres);

        getIntent().setExtrasClassLoader(Adres.class.getClassLoader());
        Adres adres = IntentCompat.getParcelableExtra(getIntent(), EXTRA_ADRES, Adres.class);
        uuid = adres == null ? UUID.randomUUID().toString() : adres.getIdAdres();
        if (savedInstanceState != null) {
            uuid = savedInstanceState.getString(STAN_UUID);
            edtMiasto.setText(savedInstanceState.getString("miasto"));
            edtUlica.setText(savedInstanceState.getString("ulica"));
            edtNumerDomu.setText(savedInstanceState.getString("numerDomu"));
            edtKodPocztowy.setText(savedInstanceState.getString("kodPocztowy"));
        } else if (adres != null) {
            edtMiasto.setText(adres.getMiasto());
            edtUlica.setText(adres.getUlica());
            edtNumerDomu.setText(adres.getNumerDomu());
            edtKodPocztowy.setText(adres.getKodPocztowy());
        }
        btnZapiszAdres.setText(adres == null ? R.string.zapisz_adres : R.string.zapisz_zmiany);
        btnUsunAdres.setVisibility(adres == null ? View.GONE : View.VISIBLE);

        btnWroc.setOnClickListener(view -> finish());
        btnZapiszAdres.setOnClickListener(view -> {
            Adres wynikAdres = new Adres(
                    edtMiasto.getText().toString(), edtUlica.getText().toString(),
                    edtNumerDomu.getText().toString(), edtKodPocztowy.getText().toString());
            wynikAdres.setIdAdres(uuid);
            Intent wynik = new Intent();
            wynik.putExtra(EXTRA_ADRES, wynikAdres);
            setResult(RESULT_OK, wynik);
            finish();
        });
        btnUsunAdres.setOnClickListener(view -> {
            Intent wynik = new Intent();
            wynik.putExtra(EXTRA_USUN_ADRES, true);
            setResult(RESULT_OK, wynik);
            finish();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STAN_UUID, uuid);
        outState.putString("miasto", edtMiasto.getText().toString());
        outState.putString("ulica", edtUlica.getText().toString());
        outState.putString("numerDomu", edtNumerDomu.getText().toString());
        outState.putString("kodPocztowy", edtKodPocztowy.getText().toString());
        super.onSaveInstanceState(outState);
    }
}
