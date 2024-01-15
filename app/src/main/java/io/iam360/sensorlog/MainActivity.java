package io.iam360.sensorlog;

import android.content.Context;
import android.Manifest;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import android.os.Build;

public class MainActivity extends AppCompatActivity {

    private Button buttonStart;
    private Button buttonStop;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private static final int REQUEST_FINE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_BACKGROUND_LOCATION_PERMISSION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStop = (Button) findViewById(R.id.buttonStop);

        buttonStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (checkLocationPermission()) {
                        startSensorService();
                    }
                    return true;
                }
                return false;
            }
        });

        buttonStop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    stopSensorService();
                    return true;
                }
                return false;
            }
        });
    }

    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return false;
        } else {
            return true;
        }
    }

    private void startSensorService() {
        startService(new Intent(MainActivity.this, SensorService.class));
        buttonStart.setEnabled(false);
        buttonStop.setEnabled(true);
    }

    private void stopSensorService() {
        stopService(new Intent(MainActivity.this, SensorService.class));
        buttonStart.setEnabled(true);
        buttonStop.setEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_FINE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Verifica si el permiso de ubicación en segundo plano es necesario (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            REQUEST_BACKGROUND_LOCATION_PERMISSION);
                } else {
                    startSensorService(); // Inicia el servicio ya que solo se necesita el permiso de ubicación precisa
                }
            } else {
                // Manejar el caso donde el usuario rechaza el permiso de ubicación precisa.
            }
        } else if (requestCode == REQUEST_BACKGROUND_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSensorService(); // Inicia el servicio, ambos permisos han sido concedidos
            } else {
                // Manejar el caso donde el usuario rechaza el permiso de ubicación en segundo plano.
                // La funcionalidad de la app podría verse limitada.
            }
        }

    }

}
