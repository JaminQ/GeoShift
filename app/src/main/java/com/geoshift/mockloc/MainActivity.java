package com.geoshift.mockloc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private Marker marker;
    private TextView tvLat, tvLng, tvAddress;
    private Button btnToggle, btnCopy, btnPaste;
    private Chip statusChip;
    private BottomSheetBehavior<View> bottomSheet;

    private double currentLat = 39.9042;
    private double currentLng = 116.4074;
    private boolean isMocking = false;
    private final OkHttpClient httpClient = new OkHttpClient();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startMockService();
                else Toast.makeText(this, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configuration.getInstance().setUserAgentValue(getPackageName());

        initViews();
        initMap();
        setupListeners();
    }

    private void initViews() {
        mapView = findViewById(R.id.mapFragment);
        tvLat = findViewById(R.id.tvLat);
        tvLng = findViewById(R.id.tvLng);
        tvAddress = findViewById(R.id.tvAddress);
        btnToggle = findViewById(R.id.btnToggle);
        btnCopy = findViewById(R.id.btnCopy);
        btnPaste = findViewById(R.id.btnPaste);
        statusChip = findViewById(R.id.statusChip);

        View bottomSheetView = findViewById(R.id.bottom_sheet);
        bottomSheet = BottomSheetBehavior.from(bottomSheetView);
        bottomSheet.setPeekHeight(270);
        bottomSheet.setHideable(false);
    }

    private void initMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(new GeoPoint(currentLat, currentLng));

        marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(currentLat, currentLng));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_marker));
        marker.setDraggable(true);
        mapView.getOverlays().add(marker);

        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override public void onMarkerDragStart(Marker m) {}
            @Override public void onMarkerDragEnd(Marker m) {
                GeoPoint gp = m.getPosition();
                updateLocation(gp.getLatitude(), gp.getLongitude());
            }
            @Override public void onMarkerDrag(Marker m) {}
        });

        mapView.addMapListener(new MapListener() {
            @Override public boolean onScroll(ScrollEvent event) { return false; }
            @Override public boolean onZoom(ZoomEvent event) { return false; }
        });

        mapView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                GeoPoint gp = (GeoPoint) mapView.getProjection().fromPixels(
                        (int) event.getX(), (int) event.getY());
                marker.setPosition(gp);
                mapView.getController().animateTo(gp);
                updateLocation(gp.getLatitude(), gp.getLongitude());
            }
            return false;
        });

        updateCoords(currentLat, currentLng);
        reverseGeocode(currentLat, currentLng);
    }

    private void updateLocation(double lat, double lng) {
        currentLat = lat;
        currentLng = lng;
        updateCoords(lat, lng);
        reverseGeocode(lat, lng);

        if (isMocking) {
            MockLocationService.pushMockLocation(MainActivity.this, lat, lng);
        }
    }

    private void updateCoords(double lat, double lng) {
        tvLat.setText(String.format("%.6f", lat));
        tvLng.setText(String.format("%.6f", lng));
    }

    private void reverseGeocode(double lat, double lng) {
        String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat="
                + lat + "&lon=" + lng + "&zoom=18&addressdetails=1";
        Request request = new Request.Builder().url(url)
                .header("User-Agent", "GeoShift/1.0")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        tvAddress.setText(lat + ", " + lng));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(body);
                        String addr = json.optString("display_name", lat + ", " + lng);
                        runOnUiThread(() -> tvAddress.setText(addr));
                    } catch (Exception e) {
                        runOnUiThread(() -> tvAddress.setText(lat + ", " + lng));
                    }
                }
            }
        });
    }

    private void setupListeners() {
        btnToggle.setOnClickListener(v -> {
            if (isMocking) {
                stopMockService();
            } else {
                showEnableMockDialog();
            }
        });

        btnCopy.setOnClickListener(v -> {
            String coords = currentLat + ", " + currentLng;
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("coords", coords);
            cm.setPrimaryClip(clip);
            Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
        });

        btnPaste.setOnClickListener(v -> {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
            if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                String text = cm.getPrimaryClip().getItemAt(0).coerceToText(MainActivity.this).toString().trim();
                parseAndSetCoords(text);
            }
        });
    }

    private void parseAndSetCoords(String text) {
        String[] parts = text.split(",");
        if (parts.length == 2) {
            try {
                double lat = Double.parseDouble(parts[0].trim());
                double lng = Double.parseDouble(parts[1].trim());
                if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
                    marker.setPosition(new GeoPoint(lat, lng));
                    mapView.getController().animateTo(new GeoPoint(lat, lng));
                    updateLocation(lat, lng);
                    Toast.makeText(this, "已设置坐标", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException ignored) {}
        }
        Toast.makeText(this, R.string.toast_invalid_coords, Toast.LENGTH_SHORT).show();
    }

    private boolean isThisAppMockProvider() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String mockApps = Settings.Secure.getString(getContentResolver(),
                        "mock_location_app");
                return mockApps != null && mockApps.equals(getPackageName());
            }
        } catch (Exception e) {
            // fall through
        }
        return false;
    }

    private void showEnableMockDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.mock_enable_title)
                .setMessage(R.string.mock_enable_desc)
                .setPositiveButton(R.string.mock_open_settings, (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startMockService() {
        Intent intent = new Intent(this, MockLocationService.class);
        intent.putExtra("lat", currentLat);
        intent.putExtra("lng", currentLng);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        isMocking = true;
        updateMockUI();
        Toast.makeText(this, R.string.toast_started, Toast.LENGTH_SHORT).show();
    }

    private void stopMockService() {
        Intent intent = new Intent(this, MockLocationService.class);
        stopService(intent);
        isMocking = false;
        updateMockUI();
        Toast.makeText(this, R.string.toast_stopped, Toast.LENGTH_SHORT).show();
    }

    private void updateMockUI() {
        runOnUiThread(() -> {
            if (isMocking) {
                btnToggle.setText(R.string.btn_stop);
                btnToggle.setIconResource(android.R.drawable.ic_media_pause);
                statusChip.setText(R.string.mock_status_active);
                statusChip.setTextColor(getColor(0x00d4aa));
                statusChip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0x00d4aa));
            } else {
                btnToggle.setText(R.string.btn_start);
                btnToggle.setIconResource(android.R.drawable.ic_popup_sync);
                statusChip.setText(R.string.mock_status_inactive);
                statusChip.setTextColor(getColor(0x7a8ba3));
                statusChip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0x2a3548));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        // Check if mock provider was enabled while away
        if (!isMocking) {
            // Could check mock status here
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isMocking) stopMockService();
    }
}
