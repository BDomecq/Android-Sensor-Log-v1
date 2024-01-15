package io.iam360.sensorlog;

import java.util.Calendar;
import android.os.Bundle;
import android.os.Environment;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.pm.PackageManager;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;
import java.io.File;

import android.database.Cursor;
import java.io.FileInputStream;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import androidx.core.app.NotificationCompat;


public class SensorService extends Service implements SensorEventListener2, LocationListener {

    private SensorManager sensorManager;
    private LocationManager locationManager;
    private boolean isRunning;
    private Writer writer;
    private Handler handler = new Handler();
    private boolean isGPSEnabled;
    private Location lastLocation;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private boolean Headers = true;
    private static final int SENSOR_DELAY_FASTEST = 0;
    private static final int UPLOADING_DELAY = 300000;

    private Uri uri;

    @Override
    public void onFlushCompleted(Sensor sensor) {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);


        if (isGPSEnabled) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            }
        }

        startNewCsvFile();
        registerSensorListeners();
        handler.postDelayed(runnable, UPLOADING_DELAY);

        startForegroundService();

        return START_STICKY;
    }

    private void startForegroundService() {
        String NOTIFICATION_CHANNEL_ID = "io.iam360.sensorlog.channel";
        String channelName = "Sensor Service Background";
        int NOTIFICATION_ID = 12345;

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Sensor Log Running")
                .setContentText("Logging sensor data in background")
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Consider changing the priority
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }


    private void registerSensorListeners() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SENSOR_DELAY_FASTEST);
    }

    private void startNewCsvFile() {
        Calendar c = Calendar.getInstance();
        String fileName = "Sensor_Log_" + c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + "-" + c.get(Calendar.MINUTE) + c.get(Calendar.SECOND) + ".csv";

        try {
            if (writer != null) {
                writer.close();
            }

            uri = createFileInMediaStore(fileName);

            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                writer = new OutputStreamWriter(outputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private Uri createFileInMediaStore(String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/SensorLogs");
        return getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String formattedTime = dateFormat.format(new Date(event.timestamp / 1000000L));
        double latitude = 0.0, longitude = 0.0;

        if (lastLocation != null) {
            latitude = lastLocation.getLatitude();
            longitude = lastLocation.getLongitude();
        }

        if(isRunning) {

            try {
                if (Headers) {
                    writer.write("timestamp; Type; X; Y; Z; W-Xbias; Y-bias; Z-bias; Latitude; Longitude\n");
                    Headers = false;
                }

                switch(event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        writer.write(String.format("%s; ACC; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f, latitude, longitude));
                        break;
                    case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                        writer.write(String.format("%s; GYRO_UN; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, event.values[0], event.values[1], event.values[2], event.values[3], event.values[4], event.values[5], latitude, longitude));
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        writer.write(String.format("%s; GYRO; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f, latitude, longitude));
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        writer.write(String.format("%s; MAG; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f, latitude, longitude));
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                        writer.write(String.format("%s; MAG_UN; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, event.values[0], event.values[1], event.values[2], 0.f, 0.f, 0.f, latitude, longitude));
                        break;
                    case Sensor.TYPE_ROTATION_VECTOR:
                        writer.write(String.format("%s; ROT; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, event.values[0], event.values[1], event.values[2], event.values[3], 0.f, 0.f, latitude, longitude));
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        handler.removeCallbacks(runnable);
        sensorManager.flush(this);
        sensorManager.unregisterListener(this);

        if (writer != null) {
            try {
                writer.close();
                writer = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (uri != null) {
            String realPath = getRealPathFromURI(uri);
            String zipFilePath = realPath.replace(".csv", ".zip");
            zipFile(realPath, zipFilePath);
        }

        super.onDestroy();

        stopForeground(true);

    }


    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                closeCurrentCsvFile();
                startNewCsvFile();
                handler.postDelayed(this, UPLOADING_DELAY);
            }
        }
    };

    private void closeCurrentCsvFile() {
        if (writer != null) {
            try {
                writer.close();
                writer = null;

                String realPath = getRealPathFromURI(uri);
                String zipFilePath = realPath.replace(".csv", ".zip");
                zipFile(realPath, zipFilePath);


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String path = null;
        String[] proj = { MediaStore.MediaColumns.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            path = cursor.getString(column_index);
        }
        cursor.close();
        return path;
    }

    private void zipFile(String filePath, String zipFilePath) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }

            fis = new FileInputStream(file);
            fos = new FileOutputStream(zipFilePath);
            zos = new ZipOutputStream(fos);
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();

            if (file.delete()) {

            } else {

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (zos != null) {
                    zos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
