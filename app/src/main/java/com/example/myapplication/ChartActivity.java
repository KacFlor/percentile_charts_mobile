package com.example.myapplication;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ChartActivity extends AppCompatActivity {

    private LineChart chart;
    private EditText inputAge, inputValue;
    private TextView tvPointCount;
    private LineDataSet userDataSet;
    private String chartType;

    private View panelTable;
    private TableLayout tableData;
    private Button btnToggleTable;
    private Button btnSaveChart;
    private boolean isTableVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        chart = findViewById(R.id.chart);
        inputAge = findViewById(R.id.inputAge);
        inputValue = findViewById(R.id.inputValue);
        tvPointCount = findViewById(R.id.tvPointCount);
        TextView titleView = findViewById(R.id.chartTitle);
        Button btnAdd = findViewById(R.id.btnAddPoint);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnBack = findViewById(R.id.btnBack);

        panelTable = findViewById(R.id.panelTable);
        tableData = findViewById(R.id.tableData);
        btnToggleTable = findViewById(R.id.btnToggleTable);
        btnSaveChart = findViewById(R.id.btnSaveChart);

        chartType = getIntent().getStringExtra("CHART_TYPE");
        String title = getIntent().getStringExtra("CHART_TITLE");
        String yLabel = getIntent().getStringExtra("Y_LABEL");

        titleView.setText(title);
        inputValue.setHint(yLabel);

        setupChartConfig();
        loadChartData(chartType);

        btnAdd.setOnClickListener(v -> addPoint());
        btnClear.setOnClickListener(v -> clearUserPoints());
        btnBack.setOnClickListener(v -> finish());
        btnToggleTable.setOnClickListener(v -> toggleTableVisibility());

        btnSaveChart.setOnClickListener(v -> saveImageToGallery());
    }

    private void saveImageToGallery() {
        if (chart.getData() == null) return;
        Bitmap bitmap = chart.getChartBitmap();

        String fileName = "Chart_" + System.currentTimeMillis() + ".png";
        OutputStream fos;

        try {
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ChartsApp");
            }

            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            if (imageUri != null) {
                fos = resolver.openOutputStream(imageUri);
                boolean saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                if (fos != null) fos.close();

                if (saved) {
                    Toast.makeText(this, "Chart saved to Gallery!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Error saving chart", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleTableVisibility() {
        if (isTableVisible) {
            panelTable.setVisibility(View.GONE);
            btnToggleTable.setText("List");
            isTableVisible = false;
        } else {
            panelTable.setVisibility(View.VISIBLE);
            btnToggleTable.setText("Hide");
            isTableVisible = true;
        }
    }

    private double[] getInterpolationParams(double[] xData, float age) {
        int idx = -1;
        for (int i = 0; i < xData.length - 1; i++) {
            if (age >= xData[i] && age <= xData[i+1]) {
                idx = i;
                break;
            }
        }
        if (idx == -1 && Math.abs(age - xData[xData.length-1]) < 0.01) {
            idx = xData.length - 2;
        }

        if (idx == -1) return null;

        double x0 = xData[idx];
        double x1 = xData[idx+1];
        double fraction = (age - x0) / (x1 - x0);

        return new double[]{idx, fraction};
    }

    private boolean isValueInPercentileRange(float age, float value) {
        ChartDataStore.SeriesData[] seriesList = ChartDataStore.getAllChartsData().get(chartType);
        if (seriesList == null || seriesList.length == 0) return true;

        double[] xData = ChartDataStore.AGE_X_AXIS;
        double[] params = getInterpolationParams(xData, age);
        if (params == null) return false;

        int idx = (int) params[0];
        double fraction = params[1];

        float minCurveValue = Float.MAX_VALUE;
        float maxCurveValue = Float.MIN_VALUE;

        for (ChartDataStore.SeriesData series : seriesList) {
            if (idx >= series.values.length - 1) continue;
            double y0 = series.values[idx];
            double y1 = series.values[idx+1];
            float interpolatedY = (float) (y0 + (y1 - y0) * fraction);

            if (interpolatedY < minCurveValue) minCurveValue = interpolatedY;
            if (interpolatedY > maxCurveValue) maxCurveValue = interpolatedY;
        }

        return value >= minCurveValue && value <= maxCurveValue;
    }

    private String getPercentileInfo(float age, float value) {
        ChartDataStore.SeriesData[] seriesList = ChartDataStore.getAllChartsData().get(chartType);
        if (seriesList == null || seriesList.length == 0) return "No data";

        double[] xData = ChartDataStore.AGE_X_AXIS;
        double[] params = getInterpolationParams(xData, age);

        if (params == null) return "Age out of range";

        int idx = (int) params[0];
        double fraction = params[1];

        String prevLabel = "";

        for (int i = 0; i < seriesList.length; i++) {
            ChartDataStore.SeriesData series = seriesList[i];

            if (idx >= series.values.length - 1) continue;

            double y0 = series.values[idx];
            double y1 = series.values[idx+1];
            float currentCurveY = (float) (y0 + (y1 - y0) * fraction);

            if (value < currentCurveY) {
                if (i == 0) {
                    return "Result: Below " + series.label;
                } else {
                    return "Result: Between " + prevLabel + " and " + series.label;
                }
            }
            prevLabel = series.label;
        }
        return "Result: Above " + prevLabel;
    }

    private void addTableRow(float age, float value) {
        TableRow row = new TableRow(this);
        row.setPadding(0, 10, 0, 10);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.0f);
        params.setMargins(2, 0, 2, 0);

        TextView tvAge = new TextView(this);
        tvAge.setText(String.valueOf(age));
        tvAge.setGravity(Gravity.CENTER);
        tvAge.setLayoutParams(params);
        row.addView(tvAge);

        TextView tvVal = new TextView(this);
        tvVal.setText(String.valueOf(value));
        tvVal.setGravity(Gravity.CENTER);
        tvVal.setLayoutParams(params);
        row.addView(tvVal);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("●");
        tvStatus.setTextSize(22f);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setLayoutParams(params);
        tvStatus.setPadding(0, 20, 0, 20);
        tvStatus.setClickable(true);

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        tvStatus.setBackgroundResource(outValue.resourceId);

        if (isValueInPercentileRange(age, value)) {
            tvStatus.setTextColor(Color.BLACK);
        } else {
            tvStatus.setTextColor(Color.RED);
        }

        tvStatus.setOnClickListener(v -> {
            String info = getPercentileInfo(age, value);
            Toast.makeText(ChartActivity.this, info, Toast.LENGTH_LONG).show();
        });

        row.addView(tvStatus);

        Button btnDel = new Button(this);
        btnDel.setText("X");
        btnDel.setTextSize(12f);
        btnDel.setTextColor(Color.WHITE);
        btnDel.setBackgroundColor(Color.RED);
        btnDel.setPadding(0, 0, 0, 0);
        btnDel.setLayoutParams(params);

        btnDel.setOnClickListener(v -> {
            removePointFromTableAndChart(age, value, row);
        });

        row.addView(btnDel);

        tableData.addView(row);
    }

    private void removePointFromTableAndChart(float age, float value, TableRow row) {
        List<Entry> entries = userDataSet.getValues();
        Entry entryToRemove = null;

        for (Entry e : entries) {
            if (Math.abs(e.getX() - age) < 0.001 && Math.abs(e.getY() - value) < 0.001) {
                entryToRemove = e;
                break;
            }
        }

        if (entryToRemove != null) {
            userDataSet.removeEntry(entryToRemove);
            userDataSet.notifyDataSetChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
            tvPointCount.setText("Points: " + userDataSet.getEntryCount());
        }

        tableData.removeView(row);
        Toast.makeText(this, "Point removed", Toast.LENGTH_SHORT).show();
    }

    private void setupChartConfig() {
        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawGridBackground(false);

        chart.setExtraLeftOffset(0f);
        chart.setExtraRightOffset(40f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setGridLineWidth(1f);
        xAxis.setAxisMinimum(3f);
        xAxis.setAxisMaximum(18f);
        xAxis.setLabelCount(16, true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setGridLineWidth(1f);
        leftAxis.setDrawLimitLinesBehindData(false);
        chart.getAxisRight().setEnabled(false);

        if (chartType != null) {
            switch (chartType) {
                case "WEIGHT_BOYS":
                case "WEIGHT_GIRLS":
                    leftAxis.setAxisMinimum(10f);
                    leftAxis.setAxisMaximum(100f);
                    leftAxis.setLabelCount(19, true);
                    break;
                case "HEIGHT_BOYS":
                case "HEIGHT_GIRLS":
                    leftAxis.setAxisMinimum(90f);
                    leftAxis.setAxisMaximum(195f);
                    leftAxis.setLabelCount(22, true);
                    break;
                case "BMI":
                    leftAxis.setAxisMinimum(13f);
                    leftAxis.setAxisMaximum(31f);
                    leftAxis.setLabelCount(19, true);
                    break;
            }
        }
    }

    private void loadChartData(String type) {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        ChartDataStore.SeriesData[] seriesList = ChartDataStore.getAllChartsData().get(type);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines();

        if (seriesList != null) {
            double[] xData = ChartDataStore.AGE_X_AXIS;

            for (ChartDataStore.SeriesData series : seriesList) {
                ArrayList<Entry> entries = new ArrayList<>();
                float lastY = 0;
                boolean hasPoints = false;

                for (int i = 0; i < xData.length; i++) {
                    if (i < series.values.length) {
                        float x = (float) xData[i];
                        float y = (float) series.values[i];
                        entries.add(new Entry(x, y));
                        lastY = y;
                        hasPoints = true;
                    }
                }

                LineDataSet set = new LineDataSet(entries, series.label);
                set.setColor(Color.BLACK);
                set.setDrawCircles(false);
                set.setLineWidth(series.isBold ? 2.5f : 1.2f);
                set.setDrawValues(false);

                if (series.isDashed) {
                    set.enableDashedLine(15f, 10f, 0f);
                }

                if (hasPoints) {
                    LimitLine ll = new LimitLine(lastY, series.label);
                    ll.setLineColor(Color.TRANSPARENT);
                    ll.setLineWidth(0f);
                    ll.setTextColor(Color.BLACK);
                    ll.setTextSize(10f);
                    ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
                    ll.setXOffset(-5f);
                    leftAxis.addLimitLine(ll);
                }
                dataSets.add(set);
            }
        }

        userDataSet = new LineDataSet(new ArrayList<>(), "User Data");
        userDataSet.setColor(Color.RED);
        userDataSet.setCircleColor(Color.RED);
        userDataSet.setLineWidth(2f);
        userDataSet.setCircleRadius(5f);
        userDataSet.setDrawValues(false);
        userDataSet.setDrawCircleHole(false);
        dataSets.add(userDataSet);

        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void addPoint() {
        String ageStr = inputAge.getText().toString();
        String valStr = inputValue.getText().toString();

        if (ageStr.isEmpty() || valStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            float age = Float.parseFloat(ageStr);
            float value = Float.parseFloat(valStr);

            if (age < 3 || age > 18) {
                inputAge.setError("Age must be between 3 and 18");
                return;
            }

            float minVal = 0;
            float maxVal = 0;
            String valueName = "Value";

            if (chartType != null) {
                switch (chartType) {
                    case "WEIGHT_BOYS":
                    case "WEIGHT_GIRLS":
                        minVal = 10f;
                        maxVal = 100f;
                        valueName = "Weight";
                        break;
                    case "HEIGHT_BOYS":
                    case "HEIGHT_GIRLS":
                        minVal = 90f;
                        maxVal = 195f;
                        valueName = "Height";
                        break;
                    case "BMI":
                        minVal = 13f;
                        maxVal = 31f;
                        valueName = "BMI";
                        break;
                }
            }

            if (value < minVal || value > maxVal) {
                inputValue.setError(valueName + " " + minVal + "-" + maxVal);
                return;
            }

            for (Entry entry : userDataSet.getValues()) {
                if (entry.getX() == age && entry.getY() == value) {
                    Toast.makeText(this, "Point already exists!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            userDataSet.addEntryOrdered(new Entry(age, value));

            addTableRow(age, value);

            userDataSet.notifyDataSetChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();

            tvPointCount.setText("Points: " + userDataSet.getEntryCount());
            inputAge.setText("");
            inputValue.setText("");

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(inputValue.getWindowToken(), 0);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearUserPoints() {
        userDataSet.clear();
        userDataSet.notifyDataSetChanged();
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
        tvPointCount.setText("Points: 0");

        int childCount = tableData.getChildCount();
        if (childCount > 1) {
            tableData.removeViews(1, childCount - 1);
        }

        Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
    }
}