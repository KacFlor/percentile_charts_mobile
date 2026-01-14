package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvTitle, tvCopyright;
    private Button btnWB, btnWG, btnHB, btnHG, btnBMI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTitle = findViewById(R.id.tvMainTitle);
        tvCopyright = findViewById(R.id.tvCopyright);

        // Obsługa zmiany języka
        Button btnPL = findViewById(R.id.btnLangPL);
        Button btnLA = findViewById(R.id.btnLangLA);

        btnPL.setOnClickListener(v -> {
            ChartDataStore.currentLanguage = "PL";
            updateUITexts();
        });

        btnLA.setOnClickListener(v -> {
            ChartDataStore.currentLanguage = "LA";
            updateUITexts();
        });

        // Konfiguracja przycisków wykresów
        setupButton(R.id.btnWeightBoys, "WEIGHT_BOYS", "BTN_WB", "Y_WEIGHT");
        setupButton(R.id.btnWeightGirls, "WEIGHT_GIRLS", "BTN_WG", "Y_WEIGHT");
        setupButton(R.id.btnHeightBoys, "HEIGHT_BOYS", "BTN_HB", "Y_HEIGHT");
        setupButton(R.id.btnHeightGirls, "HEIGHT_GIRLS", "BTN_HG", "Y_HEIGHT");
        setupButton(R.id.btnBMI, "BMI", "BTN_BMI", "Y_BMI");

        // Przypisz referencje do zmiennych globalnych, by użyć ich w updateUITexts
        btnWB = findViewById(R.id.btnWeightBoys);
        btnWG = findViewById(R.id.btnWeightGirls);
        btnHB = findViewById(R.id.btnHeightBoys);
        btnHG = findViewById(R.id.btnHeightGirls);
        btnBMI = findViewById(R.id.btnBMI);

        // Ustaw teksty na start
        updateUITexts();
    }

    private void updateUITexts() {
        tvTitle.setText(ChartDataStore.translate("SELECT_CHART"));
        tvCopyright.setText(ChartDataStore.translate("COPYRIGHT"));

        if(btnWB != null) btnWB.setText(ChartDataStore.translate("BTN_WB"));
        if(btnWG != null) btnWG.setText(ChartDataStore.translate("BTN_WG"));
        if(btnHB != null) btnHB.setText(ChartDataStore.translate("BTN_HB"));
        if(btnHG != null) btnHG.setText(ChartDataStore.translate("BTN_HG"));
        if(btnBMI != null) btnBMI.setText(ChartDataStore.translate("BTN_BMI"));
    }

    private void setupButton(int btnId, String chartType, String titleKey, String yLabelKey) {
        Button btn = findViewById(btnId);
        btn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChartActivity.class);
            intent.putExtra("CHART_TYPE", chartType);
            // Przekazujemy klucze do tłumaczenia lub od razu przetłumaczony tekst,
            // ale bezpieczniej jest przekazać klucze i niech ChartActivity sobie tłumaczy
            // lub tutaj pobrać aktualne tłumaczenie.
            intent.putExtra("CHART_TITLE", ChartDataStore.translate(titleKey));
            intent.putExtra("Y_LABEL", ChartDataStore.translate(yLabelKey));
            startActivity(intent);
        });
    }
}