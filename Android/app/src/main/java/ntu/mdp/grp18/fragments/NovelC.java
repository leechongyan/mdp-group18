package ntu.mdp.grp18.fragments;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ntu.mdp.grp18.R;


public class NovelC extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;
    private TextView directionText;
    long lastTime = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_novel_control, container, false);
        sensorManager = (SensorManager) getContext().getSystemService(getContext().SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        directionText = (TextView) v.findViewById(R.id.directionOfTilt);
        return v;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // ensure time lag between sensor event
        if(System.currentTimeMillis() - lastTime < 500)
            return;
        float x = event.values[0];
        float y = event.values[1];
        if (Math.abs(x) > Math.abs(y)) {
            if (x < -4) {
                directionText.setText("Right");
                lastTime = System.currentTimeMillis();
            }
            if (x > 4) {
                directionText.setText("Left");
                lastTime = System.currentTimeMillis();
            }
        } else {
            if (y < 1) {
                directionText.setText("Up");
                lastTime = System.currentTimeMillis();
            }
        }
        if (x > (-4) && x < (4) && y > (1)) {
            directionText.setText("Not tilt device");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister Sensor listener
        sensorManager.unregisterListener(this);
    }
}
