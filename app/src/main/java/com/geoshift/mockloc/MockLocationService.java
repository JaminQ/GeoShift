package com.geoshift.mockloc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class MockLocationService extends Service {

    public static final String CHANNEL_ID = "geoshift_mock_channel";
    private static final int NOTIFICATION_ID = 1001;

    private FusedLocationProviderClient fusedClient;
    private LocationCallback mockCallback;

    public static void updateLocation(Service service, double lat, double lng) {
        if (service instanceof MockLocationService) {
            ((MockLocationService) service).pushMockLocation(lat, lng);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        double lat = intent.getDoubleExtra("lat", 0);
        double lng = intent.getDoubleExtra("lng", 0);

        startForeground(NOTIFICATION_ID, buildNotification(lat, lng));
        startMocking(lat, lng);

        return START_STICKY;
    }

    private void startMocking(double lat, double lng) {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(3000)
                .build();

        mockCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null || result.getLastLocation() == null) return;

                Location mockLoc = new Location(result.getLastLocation());
                mockLoc.setLatitude(lat);
                mockLoc.setLongitude(lng);
                mockLoc.setAccuracy(5f);
                mockLoc.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mockLoc.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
                }
                mockLoc.setProvider("geo_shift");

                fusedClient.setMockLocation(mockLoc)
                        .addOnSuccessListener(aVoid -> {})
                        .addOnFailureListener(e -> {});
            }
        };

        fusedClient.requestLocationUpdates(request, mockCallback, Looper.getMainLooper());
    }

    public void pushMockLocation(double lat, double lng) {
        Location mockLoc = new Location("geo_shift");
        mockLoc.setLatitude(lat);
        mockLoc.setLongitude(lng);
        mockLoc.setAccuracy(5f);
        mockLoc.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLoc.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
        }

        fusedClient.setMockLocation(mockLoc);
    }

    private Notification buildNotification(double lat, double lng) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String content = String.format("%.4f, %.4f", lat, lng);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GeoShift 模拟位置运行中")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "GeoShift 模拟位置",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("持续提供模拟 GPS 位置");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedClient != null && mockCallback != null) {
            fusedClient.removeLocationUpdates(mockCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
