package io.iam360.sensorlog;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.LinkedList;


import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

//AGREGADOS
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements SensorEventListener2, LocationListener {

    SensorManager manager;
    Button buttonStart; //START!!
    Button buttonStop;  //STOP!!
    boolean isRunning;
    final String TAG = "SensorLog";
    Writer writer;

    private LocationManager locationManager;
    private boolean isGPSEnabled;
    private Location lastLocation;

    //-------------------------------------------
    // AGREGADOS !!
    //-------------------------------------------
    public static final int SENSOR_DELAY_FASTEST = 0;
    public static final int UPLOADING_DELAY = 500000;
    boolean Headears;

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public long[] previousTime = new long[10];
    public long[] elapsedTime = new long[10];

    public long[] currentTime = new long[10];



    //-------------------------------------------

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                closeCurrentCsvFile();
                startNewCsvFile();
                handler.postDelayed(this, UPLOADING_DELAY);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isRunning = false;

        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (isGPSEnabled) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            }
        }

        buttonStart = (Button)findViewById(R.id.buttonStart);
        buttonStop = (Button)findViewById(R.id.buttonStop);

        buttonStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);

                Calendar c = Calendar.getInstance();

                startNewCsvFile();

                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SENSOR_DELAY_FASTEST);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SENSOR_DELAY_FASTEST);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), SENSOR_DELAY_FASTEST);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SENSOR_DELAY_FASTEST);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SENSOR_DELAY_FASTEST);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SENSOR_DELAY_FASTEST);
                //manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), SENSOR_DELAY_FASTEST);

                isRunning = true;
                Headears = true;

                handler.postDelayed(runnable, UPLOADING_DELAY);

                return true;
            }
        });
        buttonStop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);

                isRunning = false;
                handler.removeCallbacks(runnable);

                manager.flush(MainActivity.this);
                manager.unregisterListener(MainActivity.this);
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });
    }

    //private Writer writer; // Cambia a Writer

    private void startNewCsvFile() {
        Calendar c = Calendar.getInstance();
        String fileName = "Sensor_Log_" + c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + "-" + c.get(Calendar.MINUTE) + c.get(Calendar.SECOND) + ".csv";

        try {
            if (writer != null) {
                writer.close();
            }

            Uri uri = createFileInMediaStore(fileName);
            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                writer = new OutputStreamWriter(outputStream);
            }
            Headears = true; // Indica que se deben escribir los encabezados en el nuevo archivo
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


    private void closeCurrentCsvFile() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
        //return "/storage/emulated/0/Android/io.iam360.sensorlog";
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent evt) {
        String formattedTime = dateFormat.format(new Date(evt.timestamp / 1000000L));
        double latitude = 0.0, longitude = 0.0;

        if (lastLocation != null) {
            latitude = lastLocation.getLatitude();
            longitude = lastLocation.getLongitude();
        }

        if(Headears && isRunning) {
            try {
                // Agrego latitud y longitud en los headers
                writer.write(String.format("timestamp; Type; X; Y; Z; W-Xbias; Y-bias; Z-bias; Latitude; Longitude\n"));
                Headears = false;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(isRunning) {
            try {
                switch(evt.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        writer.write(String.format("%s; ACC; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], 0.f, 0.f, 0.f, latitude, longitude));
                        break;
                    case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                        writer.write(String.format("%s; GYRO_UN; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], evt.values[3], evt.values[4], evt.values[5], latitude, longitude));
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        writer.write(String.format("%s; GYRO; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], 0.f, 0.f, 0.f, latitude, longitude));
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        writer.write(String.format("%s; MAG; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], 0.f, 0.f, 0.f, latitude, longitude));
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                        writer.write(String.format("%s; MAG_UN; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], 0.f, 0.f, 0.f, latitude, longitude));
                        break;
                    case Sensor.TYPE_ROTATION_VECTOR:
                        writer.write(String.format("%s; ROT; %f; %f; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], evt.values[3], 0.f, 0.f, latitude, longitude));
                        break;
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
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    // Manejo de onResume y onPause para GPS
    @Override
    protected void onResume() {
        super.onResume();
        if (isGPSEnabled && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
