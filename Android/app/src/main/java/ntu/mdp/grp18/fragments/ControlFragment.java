package ntu.mdp.grp18.fragments;

import android.content.Context;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import ntu.mdp.grp18.BluetoothService;
import ntu.mdp.grp18.MainActivity;
import ntu.mdp.grp18.MapCanvasView;
import ntu.mdp.grp18.R;

public class ControlFragment extends Fragment{

    final String TAG = "ControlFragment";

    final int COMMAND_FORWARD = 0;
    final int COMMAND_TURN_LEFT = 1;
    final int COMMAND_TURN_RIGHT = 2;

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

    private void initControlBtns(){
        final MapCanvasView mapCanvasView = getView().findViewById(R.id.map);

        ImageButton fwdBtn = getView().findViewById(R.id.fwd_btn);
        fwdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sendBluetoothCommand(COMMAND_FORWARD)){
                    mapCanvasView.robotMoveForward();
                }
            }
        });

        ImageButton turnRightBtn = getView().findViewById(R.id.turn_right_btn);
        turnRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sendBluetoothCommand(COMMAND_TURN_RIGHT)){
                    mapCanvasView.robotRotate(MapCanvasView.ACTION_ROTATE_RIGHT);
                }
            }
        });

        ImageButton turnLeftBtn = getView().findViewById(R.id.turn_left_btn);
        turnLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sendBluetoothCommand(COMMAND_TURN_LEFT)){
                    mapCanvasView.robotRotate(MapCanvasView.ACTION_ROTATE_LEFT);
                }
            }
        });

    }

    private boolean sendBluetoothCommand(int command){
        SharedPreferences sharedPref = getActivity().getSharedPreferences("commandSettings", Context.MODE_PRIVATE);
        String defaultValue, commandValue = null;
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
        }
        if(isBluetoothServiceBound() && commandValue != null){
            if(!getBluetoothService().write(commandValue)){
                Toast.makeText(getContext(), "Failed to send message " + commandValue, Toast.LENGTH_LONG).show();
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
}
