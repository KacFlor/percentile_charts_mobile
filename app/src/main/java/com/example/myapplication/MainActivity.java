package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupButton(R.id.btnWeightBoys, "WEIGHT_BOYS", "Weight for Age (Boys)", "Weight (kg)");
        setupButton(R.id.btnWeightGirls, "WEIGHT_GIRLS", "Weight for Age (Girls)", "Weight (kg)");
        setupButton(R.id.btnHeightBoys, "HEIGHT_BOYS", "Height for Age (Boys)", "Height (cm)");
        setupButton(R.id.btnHeightGirls, "HEIGHT_GIRLS", "Height for Age (Girls)", "Height (cm)");
        setupButton(R.id.btnBMI, "BMI", "BMI for Age", "BMI (kg/m^2)");
    }

    private void setupButton(int btnId, String chartType, String title, String yLabel) {
        Button btn = findViewById(btnId);
        btn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChartActivity.class);
            intent.putExtra("CHART_TYPE", chartType);
            intent.putExtra("CHART_TITLE", title);
            intent.putExtra("Y_LABEL", yLabel);
            startActivity(intent);
        });
    }
}