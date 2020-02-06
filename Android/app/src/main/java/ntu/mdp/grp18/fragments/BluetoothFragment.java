package ntu.mdp.grp18.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import ntu.mdp.grp18.R;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;


public class BluetoothFragment extends Fragment {

    Switch bluetoothSwitch;
    EditText deviceNameEditText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.bluetooth_fragment, container, false);

    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {

        //Bluetooth on/off control-------------------------------------------------------------------------------
        bluetoothSwitch = view.findViewById(R.id.bluetooth_switch);

        boolean isBluetoothOn = false;
        //Todo: [BE] get the state of bluetooth(on or off) from "shared preference" and put it into isBluetoothOn
        bluetoothSwitch.setChecked(isBluetoothOn);

        if(isBluetoothOn){
            onBluetoothOn(view);
        }
        else {
            onBluetoothOff(view);
        }

        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    //test
                    Log.d("debug", "switch is on");

                    onBluetoothOn(view);
                }
                else {
                    //test
                    Log.d("debug", "switch is off");

                    onBluetoothOff(view);
                }

            }
        });

        //Bluetooth device name section----------------------------------------------------------------------
        initBluetoothDeviceNameSection(view);

        //monitor bluetooth connectivity-----------------------------------------------------------------
        //Todo: [FE] create a thread that will be polling bluetooth connectivity every 1 or 2 seconds constantly

    }

    public void onBluetoothOn(View view){
        //show views that need to be shown when bluetooth is on
        setVisibilitiesWithBluetoothState(view, true);

        //init available devices section
        initBluetoothAvailableDevicesSection(view);

        //set device name to be not focusable
        setFocusableForDeviceName(view, true);

        //turn on bluetooth
        //Todo: [BT] turn on bluetooth and make our tablet visible to nearby devices, if it's not already turned on

        //store bluetooth state
        //Todo: [BE] store the bluetooth state(on) to "shared preference"


        //handle paired device section
        //init state to device being disconnected
        onDeviceDisconnected(view);

        String pairedDeviceName = "";
        //Todo: [BT] return the paired device name if there's a device that is paired with our tablet at the moment
        if(pairedDeviceName != ""){
            onDeviceConnected(view, pairedDeviceName);
        }
        else{
            //if there's no device paired with out tablet at the moment, try connect to the device we connected to last time
            //Todo: [BE] retrieve the paired device name from "shared preference" if there is one
            if(pairedDeviceName != ""){
                boolean isConnected = false;
                //Todo: [BT] try to connect to the device with the name pairedDeviceName && indicate the outcome
                //Todo: [BT] This probably needs to be done with a callback function. If that is the case, use "onDeviceConnected" as the callback, and call it when connection is successfully set up
                if(isConnected) {
                    //Todo: [FE] including pairedDeviceType
                    onDeviceConnected(view, pairedDeviceName);
                }
            }
        }

    }

    public void onBluetoothOff(View view){
        //handle disconnection
        onDeviceDisconnected(view);

        //hide views that need to be hidden when bluetooth is off
        setVisibilitiesWithBluetoothState(view, false);

        //set device name to be not focusable
        setFocusableForDeviceName(view, false);

        //store bluetooth state
        //Todo: [BE] store the bluetooth state(off) to "shared preference"

        //turn off bluetooth
        //Todo: [BT] turn off bluetooth if it's not already turned off
    }

    public void onDeviceConnected(View view, String pairedDeviceName){
        setPairedDevice(view, pairedDeviceName);

        //store paired device name
        //Todo: [BE] store the paired device name to "shared preference"

        //init testing section
        initBluetoothTestingSection(view);
    }

    public void onDeviceDisconnected(View view){
        //clear paired device section
        clearPairedDevice(view);

        //hide testing section
        LinearLayout bluetoothTestingSection = view.findViewById(R.id.bluetooth_testing_section);
        bluetoothTestingSection.setVisibility(View.GONE);
    }

    //show or hide views related to bluetooth
    public void setVisibilitiesWithBluetoothState(View view, boolean isBluetoothOn){
        LinearLayout[] linearLayouts = new LinearLayout[3];
        linearLayouts[0] = view.findViewById(R.id.paired_device_section);
        linearLayouts[1] = view.findViewById(R.id.available_devices_section);

        int visibility = isBluetoothOn ? View.VISIBLE : View.GONE;

        for(int i=0; i<linearLayouts.length; i++){
            if(linearLayouts[i] != null) {
                linearLayouts[i].setVisibility(visibility);
            }
        }
    }

    //set paired device to paired device section
    public void setPairedDevice(View view, String deviceName){
        //clear paired device container
        clearPairedDevice(view);

        LinearLayout pairedDeviceSection = view.findViewById(R.id.paired_device_section);
        LinearLayout pairedDeviceContainer = pairedDeviceSection.findViewById(R.id.paired_device_container);

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(LAYOUT_INFLATER_SERVICE);
        View pairedDeviceElement = inflater.inflate(R.layout.device_element, pairedDeviceContainer);

        //set device name for paired device element
        TextView pairedDeviceNameTextView = pairedDeviceElement.findViewById(R.id.device_element_name);
        pairedDeviceNameTextView.setText(deviceName);
    }

    //clear paired device from paired device section
    public void clearPairedDevice(View view){
        LinearLayout pairedDeviceSection = view.findViewById(R.id.paired_device_section);
        LinearLayout pairedDeviceContainer = pairedDeviceSection.findViewById(R.id.paired_device_container);
        pairedDeviceContainer.removeAllViews();
    }

    public void setFocusableForDeviceName(View view, boolean isFocusable){
        EditText deviceNameView = view.findViewById(R.id.device_name_edittext);
        deviceNameView.setFocusableInTouchMode(isFocusable);
        deviceNameView.setFocusable(isFocusable);
    }

    public void initBluetoothDeviceNameSection(View view){
        deviceNameEditText = view.findViewById(R.id.device_name_edittext);

        //Populate the right device name retrieved from memory into editText view
        String deviceName = "grp18 tablet";
        //Todo: [BE] get the deviceName from "shared preference"
        deviceNameEditText.setText(deviceName);

        //Todo: [BT] change the device name of our tablet that is shown in other devices' bluetooth lists, to 'deviceName'

        deviceNameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //Clear focus here from editText
                    deviceNameEditText.clearFocus();

                    String deviceName = deviceNameEditText.getText().toString();

                    //test
                    Log.d("debug", deviceName);

                    /**Todo: [BT] change the device name of our tablet that is shown in other devices' bluetooth lists (if that affects the connection, then after that, try to reconnect with the current paired device if there's one)
                     * Todo: [BE] save the device name using "shared preference" so that it's remembered next time
                     **/
                }

                InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                return false;
            }
        });
    }

    public void initBluetoothTestingSection(View view){
        //show testing section
        LinearLayout bluetoothTestingSection = view.findViewById(R.id.bluetooth_testing_section);
        bluetoothTestingSection.setVisibility(View.VISIBLE);

        ImageButton sendBluetoothTextButton = view.findViewById(R.id.bluetooth_testing_send_button);
        final EditText testingEditText = view.findViewById(R.id.testing_edittext);
        sendBluetoothTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String testingText = testingEditText.getText().toString();
                if(testingText.length() > 0){
                    //test
                    Toast.makeText(getContext(), testingText, Toast.LENGTH_SHORT).show();
                    //Todo: [BT] send the message
                }
            }
        });
    }

    public void initBluetoothAvailableDevicesSection(final View fragment){
        LinearLayout availableDevicesSection = fragment.findViewById(R.id.available_devices_section);

        //test
        View availableDeviceElement = availableDevicesSection.findViewById(R.id.available_device_test);
        availableDeviceElement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDeviceConnected(fragment, "test");
            }
        });
    }

}
