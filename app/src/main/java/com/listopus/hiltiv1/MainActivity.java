package com.listopus.hiltiv1;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;



import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends Activity implements SensorEventListener {

    private float lastX, lastY, lastZ, deltaX = 0, deltaY = 0, deltaZ = 0;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView currentX, currentY, currentZ, locationText, batteryStatus;
    Button getDetailsBtn;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    public Boolean isCharging, stop;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        batteryStatus = findViewById(R.id.battery_status);
        currentX = findViewById(R.id.curr_X_pos);
        currentY = findViewById(R.id.curr_Y_pos);
        currentZ = findViewById(R.id.curr_Z_pos);
        stop = false;

        requestPermission();
        initAccelerometer();
        initLocationTracing();
        getLocation();

        final Handler handler = new Handler();

        final Runnable batteryStats = new Runnable() {
            @Override
            public void run() {
                BatteryStatus();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(batteryStats);





        Runnable logfile = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(logfile);








            final long sigLocationChangeTime = 60000;
            Runnable locationStats = new Runnable() {
                @Override
                public void run() {
                        getLocation();
                        handler.postDelayed(this, sigLocationChangeTime);
                }
            };
            handler.post(locationStats);

        }





    void initAccelerometer(){
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
        }
    }

    void initLocationTracing(){
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
        }
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLocation();
        getDetailsBtn = findViewById(R.id.scan_btn);
        locationText = findViewById(R.id.location_stats);
    }


    void BatteryStatus() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = this.registerReceiver(null, filter);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (status == BatteryManager.BATTERY_STATUS_CHARGING)
            isConnected();
        else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING)
            isNotConnected();
        else if (status == BatteryManager.BATTERY_STATUS_FULL)
            batteryStatus.setText("Battery is Full");
    }

    void isConnected() {
        batteryStatus.setText("Charging");
        isCharging = true;
    }

    void isNotConnected() {
        batteryStatus.setText("Not Charging");
        isCharging = false;
        final long batteryDryTime = 3000;

        if(!isCharging){
            if (stop){
                new CountDownTimer(batteryDryTime, 1000){

                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        StopSendingUpdates();
                    }
                };
            }
        }else{
            stop = false;

        }

    }



    void StopSendingUpdates(){
        Log.i("Works", "StopSendingUpdates: 1");
    }


    void getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess( Location location) {
                if (location != null) {
                    locationText.setText("Latitude : " + location.getLatitude() + "\n" + "Longitude : " + location.getLongitude());
                }
            }

        });


    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 1);
    }


    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        displayCleanValues();
        displayCurrentValues();
        deltaX = Math.abs(lastX - event.values[0]);
        deltaY = Math.abs(lastY - event.values[1]);
        deltaZ = Math.abs(lastZ - event.values[2]);
        if (deltaX < 2)
            deltaX = 0;
        if (deltaY < 2)
            deltaY = 0;
    }

    public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues() {
        currentX.setText(Float.toString(deltaX));
        currentY.setText(Float.toString(deltaY));
        currentZ.setText(Float.toString(deltaZ));
    }




    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater();
        return true;
    }
}