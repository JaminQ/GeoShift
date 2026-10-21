package com.geoshift.mockloc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

import com.geoshift.mockloc.databinding.ActivityMainBinding;

public class MockLocationService extends Service {
    public static boolean isRunning = false;
    private static MockLocationService instance;
    private LocationManager locationManager;
    private double targetLat = 39.9042;
    private double targetLng = 116.4074;
    private Thread updateThread;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        createNotificationChannel();
        startForeground(1, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            double lat = intent.getDoubleExtra("lat", targetLat);
            double lng = intent.getDoubleExtra("lng", targetLng);
            targetLat = lat;
            targetLng = lng;
        }
        startUpdating();
        return START_STICKY;
    }

    public static void setLocation(double lat, double lng) {
        if (instance != null && instance.locationManager != null) {
            instance.targetLat = lat;
            instance.targetLng = lng;
        }
    }

    private void startUpdating() {
        if (updateThread != null) return;
        updateThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Location mockLoc = new Location(LocationManager.GPS_PROVIDER);
                    mockLoc.setLatitude(targetLat);
                    mockLoc.setLongitude(targetLng);
                    mockLoc.setAltitude(0);
                    mockLoc.setAccuracy(1);
                    mockLoc.setTime(System.currentTimeMillis());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        mockLoc.setElapsedRealtimeNanos(System.nanoTime());
                    }
                    locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLoc);
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        updateThread.start();
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, com.geoshift.mockloc.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, "geo_shift_channel")
                .setContentTitle("GeoShift Running")
                .setContentText("Mock location active")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "geo_shift_channel", "GeoShift", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (locationManager != null) {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
