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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.w3c.dom.Text;

import java.util.Map;

import ntu.mdp.grp18.BluetoothService;
import ntu.mdp.grp18.MainActivity;
import ntu.mdp.grp18.MapCanvasView;
import ntu.mdp.grp18.MapDecoder;
import ntu.mdp.grp18.R;

public class ControlFragment extends Fragment{

    final String TAG = "ControlFragment";

    static MapCanvasView mapCanvasView;

    int mode;
    public static final int MODE_SET_WP = 0;
    public static final int MODE_DEFAULT = 1;
    public static final int MODE_SET_ROBOT = 2;

    final int COMMAND_FORWARD = 0;
    final int COMMAND_TURN_LEFT = 1;
    final int COMMAND_TURN_RIGHT = 2;
    final int COMMAND_START_EXPLORATION = 3;
    final int COMMAND_START_SHORTEST_PATH = 4;
    final int COMMAND_SET_WEIGH_POINT = 5;
    final int COMMAND_SET_ROBOT = 6;

    boolean isMapAutoUpdate = true;

    String P1 = "", P2 = "";

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

        fetchMap();

        updateMapDescriptorText();
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(btMessageReceiver);

        storeMap();
    }

    private void initControlBtns(){
//        final MapCanvasView mapCanvasView = getView().findViewById(R.id.map);

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

        final Button explorationBtn = getView().findViewById(R.id.exploration_btn);
        explorationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBluetoothCommand(COMMAND_START_EXPLORATION);
                MapCanvasView mapCanvasView = getView().findViewById(R.id.map);
                mapCanvasView.clear();
                P1 = "";
                P2 = "";
                updateMapDescriptorText();

                SharedPreferences sharedPref = getActivity().getSharedPreferences("commandSettings", Context.MODE_PRIVATE);
                String debuggingModeOn = sharedPref.getString(getResources().getString(R.string.pref_debugging_mode_key), "true");
                if(debuggingModeOn.equalsIgnoreCase("false")){
                    explorationBtn.setClickable(false);
                }
            }
        });

        Button shortestBtn = getView().findViewById(R.id.fastest_path_btn);
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

        ToggleButton setRobotBtn = getView().findViewById(R.id.set_robot_btn);
        setRobotBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    setMode(MODE_SET_ROBOT);
                }
                else{
                    setMode(MODE_DEFAULT);
//                    sendBluetoothCommand(COMMAND_SET_ROBOT);
                }
            }
        });

        ToggleButton autoManualUpdateMapBtn = getView().findViewById(R.id.auto_manual_update_btn);
        autoManualUpdateMapBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switchMapUpdateMode(isChecked);
            }
        });

        Button updateMapBtn = getView().findViewById(R.id.update_map_btn);
        updateMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapCanvasView mapCanvasView = getView().findViewById(R.id.map);
                mapCanvasView.reDraw();
            }
        });
    }

    private boolean sendBluetoothCommand(int command){
        SharedPreferences sharedPref = getActivity().getSharedPreferences("commandSettings", Context.MODE_PRIVATE);
        MapCanvasView mapCanvasView;
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
                mapCanvasView = getView().findViewById(R.id.map);
                int[] unit = translateCoordinateForPc(mapCanvasView.wpPos[0], mapCanvasView.wpPos[1]);
                commandValue = unit[0] + " " + unit[1];
                commandValue = prefix + commandValue;
                break;
            case COMMAND_SET_ROBOT:
                prefix = sharedPref.getString(getResources().getString(R.string.pref_pc_prefix_key), null);
                prefix += "rp";
                mapCanvasView = getView().findViewById(R.id.map);
                unit = translateCoordinateForPc(mapCanvasView.robotPos[0]+1, mapCanvasView.robotPos[1]+1);
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
            SharedPreferences sharedPref = getActivity().getSharedPreferences("commandSettings", Context.MODE_PRIVATE);
            String data = intent.getStringExtra(BluetoothService.EXTRA_MESSAGE);
            String mapDescriptorPrefix = sharedPref.getString(getResources().getString(R.string.pref_map_descriptor_prefix_key), "md");
            String imgPrefix = sharedPref.getString(getResources().getString(R.string.pref_img_prefix_key), "img");
            if(mapDescriptorPrefix == null || imgPrefix.length() == 0){
                mapDescriptorPrefix = "md";
            }
            if(imgPrefix == null || imgPrefix.length() == 0){
                imgPrefix = "img";
            }
            if(data.startsWith(mapDescriptorPrefix)){
                    processMapAndRobotDescriptorString(data.substring(mapDescriptorPrefix.length()));
            }
            else if(data.startsWith(imgPrefix)){
                processImageString(data.substring((imgPrefix.length())));
            }
        }
    };

    private void processMapAndRobotDescriptorString(String data){
        String[] splitData = data.split(" ");
        MapCanvasView mapCanvasView = getView().findViewById(R.id.map);

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
        P1 = splitData[0];
        P2 = splitData[1];
        updateMapDescriptorText();
        int[][] mapMatrix = MapDecoder.convertToMap(splitData[0], splitData[1]);

        int[][] mapMatrixFlipped = new int[15][20];
        for(int j=0; j<mapMatrix[0].length; j++){
            for(int i=0; i<mapMatrix.length; i++){
                mapMatrixFlipped[j][i] = mapMatrix[i][j];
            }
        }

        mapCanvasView.setMap(mapMatrixFlipped);

//        for(int j=0; j<mapMatrix2[0].length; j++){
//            String row = "";
//            for(int i=0; i<mapMatrix2.length; i++){
//                row += mapMatrix2[i][j];
//            }
//            Log.d("matrix", "role " + j + ": " + row);
//        }

        //redraw
        if(isMapAutoUpdate){
            mapCanvasView.reDraw();
        }
    }

    private void setMode(int mode){
        this.mode = mode;

        switch (mode){
            case MODE_SET_WP:
            case MODE_SET_ROBOT:
                //todo disable other buttons
                setMapTouchable(true, mode);
                break;
            case MODE_DEFAULT:
                setMapTouchable(false, mode);
                break;
        }
    }

    private void setMapTouchable(boolean touchable, int mode){
        MapCanvasView mapCanvasView = getView().findViewById(R.id.map);
        mapCanvasView.setMapTouchable(touchable);
        mapCanvasView.setTouchMode(mode);
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

    private void storeMap(){
        MapCanvasView mapCanvasView = getView().findViewById(R.id.map);
        ControlFragment.mapCanvasView = mapCanvasView;
    }

    private void fetchMap(){
        MapCanvasView mapCanvasView = getView().findViewById(R.id.map);
        mapCanvasView.fetchMap(ControlFragment.mapCanvasView);
    }

    private void switchMapUpdateMode(boolean isManual){
        Button updateMapBtn = getView().findViewById(R.id.update_map_btn);
        if(isManual){
            updateMapBtn.setClickable(true);
            updateMapBtn.setBackground(getResources().getDrawable(R.drawable.rounded_button));
            isMapAutoUpdate = false;
        }
        else{
            updateMapBtn.setClickable(false);
            updateMapBtn.setBackground(getResources().getDrawable(R.drawable.rounded_button_unclickable));
            isMapAutoUpdate = true;
        }
    }

    private void processImageString(String imageString){
        String[] imageStrings = imageString.split("\\|");
        String[] imageStringSplit;

        MapCanvasView mapCanvasView = getView().findViewById(R.id.map);

        int[] imageInfo;
        for(int j=0; j<imageStrings.length; j++){
            imageStringSplit = imageStrings[j].split(" ");

            imageInfo = new int[3];

            for(int i=0; i<3; i++){
                Log.d(TAG, "imageStringSplit:" + imageStringSplit[i]);
                imageInfo[i] = Integer.parseInt(imageStringSplit[i]);
            }

            mapCanvasView.setImagePos(imageInfo[0], imageInfo[2], MapCanvasView.NUMBER_OF_UNIT_ON_Y - 1 - imageInfo[1]);
        }

        adjustImagePosition();
        updateMapDescriptorText();
        mapCanvasView.reDraw();
    }

    private void updateMapDescriptorText(){
        String imageString = "";
        mapCanvasView = getView().findViewById(R.id.map);
        for(int i=0; i<15; i++){
            if(mapCanvasView.imagePos[i][0] == -1){
                continue;
            }
            String imageName;
            switch(i){
                case 0:
                    imageName = "red_a";
                    break;
                case 1:
                    imageName = "red_arrow";
                    break;
                case 2:
                    imageName = "red_three";
                    break;
                case 3:
                    imageName = "green_arrow";
                    break;
                case 4:
                    imageName = "green_b";
                    break;
                case 5:
                    imageName = "green_two";
                    break;
                case 6:
                    imageName = "blue_arrow";
                    break;
                case 7:
                    imageName = "blue_d";
                    break;
                case 8:
                    imageName = "blue_one";
                    break;
                case 9:
                    imageName = "white_arrow";
                    break;
                case 10:
                    imageName = "white_c";
                    break;
                case 11:
                    imageName = "white_four";
                    break;
                case 12:
                    imageName = "yellow_circle";
                    break;
                case 13:
                    imageName = "yellow_e";
                    break;
                case 14:
                    imageName = "yellow_five";
                    break;
                default:
                    imageName = "";
                    break;

            }

            imageString += ("\n" + imageName + ": (" + mapCanvasView.imagePos[i][0] + "," + (mapCanvasView.NUMBER_OF_UNIT_ON_Y - 1 - mapCanvasView.imagePos[i][1]) + ")");

        }

        TextView mapDescriptorTextView = getView().findViewById(R.id.map_descriptor_text_view);
        String text = "P1: \n" + P1 + "\nP2: \n" + P2 + "\nImage: " + imageString;
        mapDescriptorTextView.setText(text);
    }

    private void adjustImagePosition(){
        mapCanvasView = getView().findViewById(R.id.map);
        int[][] map = mapCanvasView.map;
        int[][] imagePos = mapCanvasView.imagePos;

        for(int i=0; i<imagePos.length; i++){
            int x = imagePos[i][0];
            int y = imagePos[i][1];

            if(x == -1){
                continue;
            }

            if(x < 0){
                x = 0;
            }

            if(y < 0){
                y = 0;
            }

            if(x > MapCanvasView.NUMBER_OF_UNIT_ON_X - 1){
                x = MapCanvasView.NUMBER_OF_UNIT_ON_X - 1;
            }

            if(y > MapCanvasView.NUMBER_OF_UNIT_ON_Y - 1){
                y = MapCanvasView.NUMBER_OF_UNIT_ON_Y - 1;
            }

            if(map[x][y] == MapCanvasView.OBSTACLE){
                boolean hasOverlap = false;
                for(int n=0; n<imagePos.length; n++){
                    if(n == i){
                        continue;
                    }
                    if(imagePos[n][0] == x && imagePos[n][1] == y){
                        hasOverlap = true;
                        break;
                    }
                }

                if(!hasOverlap){
                    continue;
                }
            }

            for(int j=1; j<5; j++){
                for(int px=x-j; px<=x+j; px++){
                    if(px < 0 || px >= MapCanvasView.NUMBER_OF_UNIT_ON_X){
                        continue;
                    }
                    for(int py=y-j; py<=y+j; py++) {
                        if(py < 0 || py >= MapCanvasView.NUMBER_OF_UNIT_ON_Y){
                            continue;
                        }
                        if(map[px][py] == MapCanvasView.OBSTACLE && !mapCanvasView.hasImage(px, py)){
                            mapCanvasView.setImagePos(i, px, py);
                            j = 5;
                            break;
                        }
                    }
                    if(j >= 5){
                        break;
                    }
                }
            }
        }
    }

}
