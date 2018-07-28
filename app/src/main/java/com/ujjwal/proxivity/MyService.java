package com.ujjwal.proxivity;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

public class MyService extends Service {

    private final String TAG = "Testing";
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private Sensor accelerometerSensor;
    private SensorEventListener accelerometerSensorListener;
    private SensorEventListener proximitySensorListener;
    private SensorEventListener proximityScreenOn;
    Display display;
    PowerManager pm;
    PowerManager.WakeLock wl;

    class MyServiceBinder extends Binder {
        public MyService getService() {
            return MyService.this;
        }
    }

    private IBinder binder = new MyServiceBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "In-onCreate on threadID "+Thread.currentThread().getId());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (proximitySensor == null) {
            Toast.makeText(this, "Proximity sensor unavailable.", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
        if (accelerometerSensor == null) {
            Toast.makeText(this, "Accelerometer unavailable.", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
        proximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] < proximitySensor.getMaximumRange()) {
                    sensorManager.registerListener(proximityScreenOn, proximitySensor,
                            SensorManager.SENSOR_DELAY_NORMAL);
                    sensorManager.unregisterListener(proximitySensorListener, proximitySensor);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        proximityScreenOn = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] > 0) {
                    wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "My Tag");
                    wl.acquire();
                    if (wl.isHeld()) wl.release();
                    sensorManager.registerListener(accelerometerSensorListener, accelerometerSensor,
                            SensorManager.SENSOR_DELAY_NORMAL);
                    sensorManager.unregisterListener(proximityScreenOn, proximitySensor);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        accelerometerSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
//                if (Build.VERSION.SDK_INT > 19)
//                    Log.d(TAG, "inService: "+ display.getState());
                if (event.values[1] < -0.4) {
                    DevicePolicyManager policyManager = (DevicePolicyManager) getApplicationContext()
                            .getSystemService(Context.DEVICE_POLICY_SERVICE);
                    ComponentName adminReceiver = new ComponentName(getApplicationContext(),
                            ScreenOffAdminReceiver.class);
                    boolean admin = policyManager.isAdminActive(adminReceiver);
                    if (admin) {
                        Log.i(TAG, "Going to sleep now.");
                        sensorManager.registerListener(proximitySensorListener, proximitySensor,
                                SensorManager.SENSOR_DELAY_NORMAL);
                        sensorManager.unregisterListener(accelerometerSensorListener, accelerometerSensor);
                        policyManager.lockNow();

                    } else {
                        Log.i(TAG, "Not an admin");
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        if (Build.VERSION.SDK_INT > 19 && display.getState() == Display.STATE_OFF) sensorManager.registerListener(proximitySensorListener, proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL);
        if (Build.VERSION.SDK_INT > 19 && display.getState() == Display.STATE_ON) sensorManager.registerListener(accelerometerSensorListener, accelerometerSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "In onBind");
        // TODO: Return the communication channel to the service.
        return binder;
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(proximitySensorListener, proximitySensor);
        sensorManager.unregisterListener(accelerometerSensorListener, accelerometerSensor);
        Log.d(TAG, " In onDestroy");
        super.onDestroy();
        Toast.makeText(this, "Service stopped.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, " In onStartCommand");
        new Thread(new Runnable() {
            @Override
            public void run() {
                onCreate();
            }
        }).start();
        return START_STICKY;
    }
}
