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
import android.os.Looper;

import androidx.core.app.NotificationCompat;

public class MockLocationService extends Service {

    public static final String CHANNEL_ID = "geoshift_mock_channel";
    private static final int NOTIFICATION_ID = 1001;

    private LocationManager locationManager;
    private android.location.LocationListener locationListener;

    public static void updateLocation(Service service, double lat, double lng) {
        if (service instanceof MockLocationService) {
            ((MockLocationService) service).pushMockLocation(lat, lng);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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
        locationListener = new android.location.LocationListener(LocationManager.GPS_PROVIDER) {
            @Override
            public void onLocationChanged(Location realLocation) {
                Location mockLoc = new Location(realLocation);
                mockLoc.setLatitude(lat);
                mockLoc.setLongitude(lng);
                mockLoc.setAccuracy(5f);
                mockLoc.setTime(System.currentTimeMillis());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mockLoc.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
                }
                mockLoc.setProvider(LocationManager.GPS_PROVIDER);

                locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLoc);
            }

            @Override
            public void onStatusChanged(String provider, int status, Intent extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            locationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false, false, false, false, true, true, true, 0, 0
            );
            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, 0f,
                locationListener,
                Looper.getMainLooper()
            );
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void pushMockLocation(double lat, double lng) {
        Location mockLoc = new Location(LocationManager.GPS_PROVIDER);
        mockLoc.setLatitude(lat);
        mockLoc.setLongitude(lng);
        mockLoc.setAccuracy(5f);
        mockLoc.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLoc.setElapsedRealtimeNanos(android.os.SystemClock.elapsedRealtimeNanos());
        }

        try {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLoc);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
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
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
