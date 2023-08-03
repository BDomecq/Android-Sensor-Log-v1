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

//AGREGADOS
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements SensorEventListener2 {

    SensorManager manager;
    Button buttonStart; //START!!
    Button buttonStop;  //STOP!!
    boolean isRunning;
    final String TAG = "SensorLog";
    FileWriter writer;

    //-------------------------------------------
    // AGREGADOS !!
    //-------------------------------------------
    public static final int SENSOR_DELAY_FASTEST = 0;
    boolean Headears;

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public long[] previousTime = new long[10];
    public long[] elapsedTime = new long[10];

    public long[] currentTime = new long[10];



    //-------------------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isRunning = false;

        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        buttonStart = (Button)findViewById(R.id.buttonStart);
        buttonStop = (Button)findViewById(R.id.buttonStop);

        buttonStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);

                Calendar c = Calendar.getInstance();

                Log.d(TAG, "Writing to " + getStorageDir());


                try {
                    //writer = new FileWriter(new File(getStorageDir(), "Sensor_Log_" + System.currentTimeMillis() + ".csv"));
                    //modificado
                    writer = new FileWriter(new File(getStorageDir(), "Sensor_Log_" + c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1)
                            + "-" + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR_OF_DAY) + "-" + c.get(Calendar.MINUTE) +
                            c.get(Calendar.SECOND) + ".csv"));

                } catch (IOException e) {
                    e.printStackTrace();
                }

                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SENSOR_DELAY_FASTEST);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SENSOR_DELAY_FASTEST);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), SENSOR_DELAY_FASTEST);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SENSOR_DELAY_FASTEST);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED), SENSOR_DELAY_FASTEST);
                manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SENSOR_DELAY_FASTEST);
                //manager.registerListener(MainActivity.this, manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), SENSOR_DELAY_FASTEST);

                isRunning = true;
                Headears = true;
                return true;
            }
        });
        buttonStop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);
                isRunning = false;
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

    private String getStorageDir() {
        return this.getExternalFilesDir(null).getAbsolutePath();
      //  return "/storage/emulated/0/Android/data/com.iam360.sensorlog/";
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent evt) {

        String formattedTime = dateFormat.format(new Date(evt.timestamp / 1000000L));

        if(Headears && isRunning) {
            try {
                //escribo los headers
                writer.write(String.format("timestamp; Type; X; Y; Z; W-Xbias; Y-bias; Z-bias\n"));
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
                        //currentTime[0] = System.nanoTime();
                        //elapsedTime[0] = (currentTime[0] - previousTime[0]) /1000; //microsegundos
                        writer.write(String.format("%s; ACC; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], 0.f, 0.f, 0.f));
                        break;
                    case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                        writer.write(String.format("%s; GYRO_UN; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], evt.values[3], evt.values[4], evt.values[5]));
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        writer.write(String.format("%s; GYRO; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], 0.f, 0.f, 0.f));
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        writer.write(String.format("%s; MAG; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], 0.f, 0.f, 0.f));
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                        writer.write(String.format("%s; MAG_UN; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], 0.f, 0.f, 0.f));
                        break;
                    case Sensor.TYPE_ROTATION_VECTOR:
                        writer.write(String.format("%s; ROT; %f; %f; %f; %f; %f; %f\n", formattedTime, evt.values[0], evt.values[1], evt.values[2], evt.values[3], 0.f, 0.f));
                        break;

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
