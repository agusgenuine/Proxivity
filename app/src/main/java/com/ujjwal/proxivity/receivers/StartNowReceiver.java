package com.ujjwal.proxivity.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.widget.Toast;

import com.ujjwal.proxivity.ScreenOnOffService;

public class StartNowReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Toast.makeText(context, "You started the service now.", Toast.LENGTH_SHORT).show();
            ScreenOnOffService.sensorManager.registerListener(ScreenOnOffService.proximitySensorListener, ScreenOnOffService.proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            ScreenOnOffService.sensorManager.registerListener(ScreenOnOffService.accelerometerSensorListener, ScreenOnOffService.accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

            if (ScreenOnOffService.timer != null) ScreenOnOffService.timer.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}