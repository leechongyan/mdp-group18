package ntu.mdp.grp18.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Map;

import ntu.mdp.grp18.BluetoothService;
import ntu.mdp.grp18.MainActivity;
import ntu.mdp.grp18.MapCanvasView;
import ntu.mdp.grp18.MapDecoder;
import ntu.mdp.grp18.R;

public class ControlFragment extends Fragment{

    final String TAG = "ControlFragment";

    int mode;

    final int MODE_SET_WP = 0;
    final int MODE_DEFAULT = 1;

    final int COMMAND_FORWARD = 0;
    final int COMMAND_TURN_LEFT = 1;
    final int COMMAND_TURN_RIGHT = 2;
    final int COMMAND_START_EXPLORATION = 3;
    final int COMMAND_START_SHORTEST_PATH = 4;
    final int COMMAND_SET_WEIGH_POINT = 5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_control, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initControlBtns();
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter messageReceiveFilter = new IntentFilter(BluetoothService.ACTION_BLUETOOTH_MESSAGE_RECEIVED);
        getActivity().registerReceiver(btMessageReceiver, messageReceiveFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(btMessageReceiver);
    }

    private void initControlBtns(){
        final MapCanvasView mapCanvasView = getView().findViewById(R.id.map);

//        ImageButton fwdBtn = getView().findViewById(R.id.fwd_btn);
//        fwdBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(sendBluetoothCommand(COMMAND_FORWARD)){
//                    mapCanvasView.robotMoveForward();
//                }
//            }
//        });
//
//        ImageButton turnRightBtn = getView().findViewById(R.id.turn_right_btn);
//        turnRightBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(sendBluetoothCommand(COMMAND_TURN_RIGHT)){
//                    mapCanvasView.robotRotate(MapCanvasView.ACTION_ROTATE_RIGHT);
//                }
//            }
//        });
//
//        ImageButton turnLeftBtn = getView().findViewById(R.id.turn_left_btn);
//        turnLeftBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(sendBluetoothCommand(COMMAND_TURN_LEFT)){
//                    mapCanvasView.robotRotate(MapCanvasView.ACTION_ROTATE_LEFT);
//                }
//            }
//        });

        Button explorationBtn = getView().findViewById(R.id.exploration_btn);
        explorationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBluetoothCommand(COMMAND_START_EXPLORATION);
            }
        });

        Button shortestBtn = getView().findViewById(R.id.shortest_path_btn);
        shortestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBluetoothCommand(COMMAND_START_SHORTEST_PATH);
            }
        });

        ToggleButton setWpBtn = getView().findViewById(R.id.set_wp_btn);
        setWpBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    setMode(MODE_SET_WP);
                }
                else{
                    setMode(MODE_DEFAULT);
                    sendBluetoothCommand(COMMAND_SET_WEIGH_POINT);
                }
            }
        });

    }

    private boolean sendBluetoothCommand(int command){
        SharedPreferences sharedPref = getActivity().getSharedPreferences("commandSettings", Context.MODE_PRIVATE);
        String defaultValue, commandValue = null, prefix = null;
        switch (command){
            case COMMAND_FORWARD:
                defaultValue = getResources().getString(R.string.pref_forward_default);
                commandValue = sharedPref.getString("prefforward", defaultValue);
                break;
            case COMMAND_TURN_LEFT:
                defaultValue = getResources().getString(R.string.pref_rotate_left_default);
                commandValue = sharedPref.getString("prefrotateleft", defaultValue);
                break;
            case COMMAND_TURN_RIGHT:
                defaultValue = getResources().getString(R.string.pref_rotate_right_default);
                commandValue = sharedPref.getString("prefrotateright", defaultValue);
                break;
            case COMMAND_START_EXPLORATION:
                defaultValue = getResources().getString(R.string.pref_begin_exploration_default);
                commandValue = sharedPref.getString(getResources().getString(R.string.pref_begin_exploration_key), defaultValue);
                prefix = sharedPref.getString(getResources().getString(R.string.pref_pc_prefix_key), null);
                commandValue = prefix + commandValue;
                break;
            case COMMAND_START_SHORTEST_PATH:
                defaultValue = getResources().getString(R.string.pref_begin_fastest_path_default);
                commandValue = sharedPref.getString(getResources().getString(R.string.pref_begin_fastest_path_key), defaultValue);
                prefix = sharedPref.getString(getResources().getString(R.string.pref_pc_prefix_key), null);
                commandValue = prefix + commandValue;
                break;
            case COMMAND_SET_WEIGH_POINT:
                prefix = sharedPref.getString(getResources().getString(R.string.pref_pc_prefix_key), null);
                prefix += sharedPref.getString(getResources().getString(R.string.pref_wp_prefix_key), null);
                int[] unit = translateCoordinateForPc(MapCanvasView.wpPos[0], MapCanvasView.wpPos[1]);
                commandValue = unit[0] + " " + unit[1];
                commandValue = prefix + commandValue;
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

    private BluetoothService getBluetoothService(){
        return ((MainActivity)getActivity()).getBtService();
    }

    private boolean isBluetoothServiceBound(){
        return (getBluetoothService() != null);

    }

    private final BroadcastReceiver btMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra(BluetoothService.EXTRA_MESSAGE);
            if(data.startsWith("md")){
                processMapAndRobotDescriptorString(data.substring(2));
            }
        }
    };

    private void processMapAndRobotDescriptorString(String data){
        String[] splitData = data.split(" ");
        final MapCanvasView mapCanvasView = getView().findViewById(R.id.map);

        //robot position
        mapCanvasView.setRobotPos(Integer.parseInt(splitData[3]) - 1,MapCanvasView.NUMBER_OF_UNIT_ON_Y - Integer.parseInt(splitData[2]) - 1 - 1);

        //robot direction
        int direction;
        switch (splitData[4]){
            case "e":
            case "E":
                direction = MapCanvasView.DIRECTION_RIGHT;
                break;
            case "n":
            case "N":
                direction = MapCanvasView.DIRECTION_FRONT;
                break;
            case "w":
            case "W":
                direction = MapCanvasView.DIRECTION_LEFT;
                break;
            case "s":
            case "S":
                direction = MapCanvasView.DIRECTION_BACK;
                break;
            default:
                direction = MapCanvasView.DIRECTION_DEFAULT;
                break;
        }
        mapCanvasView.setRobotDirection(direction);

        //map
        int[][] mapMatrix = MapDecoder.convertToMap(splitData[0], splitData[1]);

        int[][] mapMatrix2 = new int[15][20];
        for(int j=0; j<mapMatrix[0].length; j++){
            for(int i=0; i<mapMatrix.length; i++){
                mapMatrix2[j][i] = mapMatrix[i][j];
            }
        }

        mapCanvasView.setMap(mapMatrix2);

//        for(int j=0; j<mapMatrix2[0].length; j++){
//            String row = "";
//            for(int i=0; i<mapMatrix2.length; i++){
//                row += mapMatrix2[i][j];
//            }
//            Log.d("matrix", "role " + j + ": " + row);
//        }

        //redraw
        mapCanvasView.update();
    }

    private void setMode(int mode){
        this.mode = mode;

        switch (mode){
            case MODE_SET_WP:
                //todo disable other buttons
                setMapTouchable(true);
                break;
            case MODE_DEFAULT:
                setMapTouchable(false);
        }
    }

    private void setMapTouchable(boolean touchable){
        MapCanvasView mapCanvasView = getView().findViewById(R.id.map);
        mapCanvasView.setMapTouchable(touchable);
    }

    private int[] translateCoordinateForPc(int x, int y){
        if(x == -1 || y == -1){
            return new int[]{-1, -1};
        }
        int[] unit = new int[2];
        unit[0] = MapCanvasView.NUMBER_OF_UNIT_ON_Y - 1 - y;
        unit[1] = x;
        return unit;
    }
}
