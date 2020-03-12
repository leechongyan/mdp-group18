package ntu.mdp.grp18.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import ntu.mdp.grp18.BluetoothService;
import ntu.mdp.grp18.MainActivity;
import ntu.mdp.grp18.MapCanvasView;
import ntu.mdp.grp18.R;

public class DriveFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;
    long lastTime = 0;

    final int COMMAND_TURN_LEFT = 1, COMMAND_TURN_RIGHT = 2, COMMAND_FORWARD = 3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sensorManager = (SensorManager) getContext().getSystemService(getContext().SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        return inflater.inflate(R.layout.fragment_drive, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter messageReceiveFilter = new IntentFilter(BluetoothService.ACTION_BLUETOOTH_MESSAGE_RECEIVED);
        getActivity().registerReceiver(btMessageReceiver, messageReceiveFilter);

        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(btMessageReceiver);

        sensorManager.unregisterListener(this);
    }

    private final BroadcastReceiver btMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPref = getActivity().getSharedPreferences("commandSettings", Context.MODE_PRIVATE);
            String data = intent.getStringExtra(BluetoothService.EXTRA_MESSAGE);
            String robotStatePrefix = sharedPref.getString(getResources().getString(R.string.pref_robot_state_prefix_key), "rs");
            if(robotStatePrefix == null || robotStatePrefix.length() == 0){
                robotStatePrefix = "rs";
            }
            if(data.startsWith(robotStatePrefix)){
                setRobotState(data.substring(robotStatePrefix.length()));
            }
        }
    };

    private void setRobotState(String state){
        TextView robotStateTextView = getView().findViewById(R.id.robot_state_text_view);
        robotStateTextView.setText(state);
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
                lastTime = System.currentTimeMillis();
                sendBluetoothCommand(COMMAND_TURN_RIGHT);
            }
            if (x > 4) {
                lastTime = System.currentTimeMillis();
                sendBluetoothCommand(COMMAND_TURN_LEFT);
            }
        } else {
            if (y < 1) {
                lastTime = System.currentTimeMillis();
                sendBluetoothCommand(COMMAND_FORWARD);
            }
        }
        if (x > (-4) && x < (4) && y > (1)) {
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private BluetoothService getBluetoothService(){
        return ((MainActivity)getActivity()).getBtService();
    }

    private boolean isBluetoothServiceBound(){
        return (getBluetoothService() != null);

    }

    private boolean sendBluetoothCommand(int command){
        SharedPreferences sharedPref = getActivity().getSharedPreferences("commandSettings", Context.MODE_PRIVATE);
        String defaultValue, commandValue = null, prefix = null;
        prefix = sharedPref.getString(getResources().getString(R.string.pref_arduino_prefix_key), null);
        switch (command){
            case COMMAND_FORWARD:
                defaultValue = getResources().getString(R.string.pref_forward_default);
                commandValue = prefix + sharedPref.getString("prefforward", defaultValue);
                break;
            case COMMAND_TURN_LEFT:
                defaultValue = getResources().getString(R.string.pref_rotate_left_default);
                commandValue = prefix + sharedPref.getString("prefrotateleft", defaultValue);
                break;
            case COMMAND_TURN_RIGHT:
                defaultValue = getResources().getString(R.string.pref_rotate_right_default);
                commandValue = prefix + sharedPref.getString("prefrotateright", defaultValue);
                break;
        }
        if(isBluetoothServiceBound() && commandValue != null){
            if(!getBluetoothService().write(commandValue)){
                Toast.makeText(getContext(), "Failed to send message " + commandValue, Toast.LENGTH_SHORT).show();
                return false;
            }
            else{
                Toast.makeText(getContext(), "Message " + commandValue + " sent", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return false;
    }
}
