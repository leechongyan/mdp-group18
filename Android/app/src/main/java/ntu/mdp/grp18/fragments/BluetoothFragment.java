package ntu.mdp.grp18.fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import ntu.mdp.grp18.BluetoothService;
import ntu.mdp.grp18.MainActivity;
import ntu.mdp.grp18.R;

import static android.app.Activity.RESULT_CANCELED;
import static android.content.Context.LAYOUT_INFLATER_SERVICE;


public class BluetoothFragment extends Fragment implements OnBluetoothServiceBoundListener{

    final String  TAG = "BluetoothFragment";

    Switch bluetoothSwitch;
    EditText deviceNameEditText;

    /**
     * Listeners------------------------------------------------------------------------------------
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.bluetooth_fragment, container, false);

    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {

        //Bluetooth on/off control
        initBluetoothSwitch(view);

        //Bluetooth device name section
        initBluetoothDeviceNameSection(view);

        //Bluetooth paired devices section
        initPairedDevicesSection(view);

        //Bluetooth testing section
        initBluetoothTestingSection(view);

        //Bluetooth available devices section
        initBluetoothAvailableDevicesSection(view);

    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
        boolean isBluetoothOn = false;
        if(isBtServiceBound()){
            isBluetoothOn = getBtService().isBluetoothOn();
        }
        if(isBluetoothOn){
            onBluetoothOn(getView());
        }
        else{
            onBluetoothOff(getView());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter stateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(stateChangeReceiver, stateChangeFilter);

        IntentFilter deviceFoundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(deviceFoundReceiver, deviceFoundFilter);

        IntentFilter connectionChangeFilter = new IntentFilter(BluetoothService.ACTION_BLUETOOTH_CONNECTION_CHANGED);
        getActivity().registerReceiver(connectionChangeReceiver, connectionChangeFilter);

        IntentFilter deviceBoundFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(deviceBoundReceiver, deviceBoundFilter);

        IntentFilter messageReceiveFilter = new IntentFilter(BluetoothService.ACTION_BLUETOOTH_MESSAGE_RECEIVED);
        getActivity().registerReceiver(btMessageReceiver, messageReceiveFilter);

    }

    @Override
    public void onPause() {
        super.onPause();
        //Unregister for broadcasts
        getActivity().unregisterReceiver(stateChangeReceiver);
        getActivity().unregisterReceiver(deviceFoundReceiver);
        getActivity().unregisterReceiver(connectionChangeReceiver);
        getActivity().unregisterReceiver(deviceBoundReceiver);
        getActivity().unregisterReceiver(btMessageReceiver);

        //cancel discovery
        if(isBtServiceBound()){
            getBtService().cancelDiscovery();
        }
    }

    public void onBluetoothSwitchOn(){
        //turn on bluetooth
        if(isBtServiceBound()){
            getBtService().startBluetooth(this);
        }
    }

    public void onBluetoothSwitchOff(){
        //turn off bluetooth
        if(isBtServiceBound()){
            getBtService().stopBluetooth();
        }
        //test
        onBluetoothOff(getView());
    }

    //In case user denies the change of bluetooth status
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data); comment this unless you want to pass your result to the activity.
        if(resultCode == RESULT_CANCELED){
            onBluetoothOff(getView());
            //test
            Log.d("debug", "result_canceled");
        }
        else{
            onBluetoothOn(getView());
            Log.d("debug", "result_ok");
        }
    }

    public void onBluetoothServiceBound(){
        boolean isBluetoothOn;
        isBluetoothOn = getBtService().isBluetoothOn();
        if(isBluetoothOn){
            onBluetoothOn(getView());
        }
        else{
            onBluetoothOff(getView());
        }
    }

    public void onBluetoothOn(View view){

        //set if not already done
        bluetoothSwitch.setChecked(true);

        //show views that need to be shown when bluetooth is on
        setVisibilitiesWithBluetoothState(view, BluetoothAdapter.STATE_ON);

        //set our device name to be not focusable
        setFocusableForDeviceName(view, true);

        //set paired devices
        setPairedDevices(view);

        clearAvailableDevices(view);

    }

    public void onBluetoothOff(View view){

        //set if not already done
        bluetoothSwitch.setChecked(false);

        //hide views that need to be hidden when bluetooth is off
        setVisibilitiesWithBluetoothState(view, BluetoothAdapter.STATE_OFF);

        //set device name to be not focusable
        setFocusableForDeviceName(view, false);

    }

    private final BroadcastReceiver stateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        onBluetoothOff(getView());
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //test
                        Log.d("debug", "Bluetooth turning off");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        onBluetoothOn(getView());
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        //test
                        Log.d("debug", "Bluetooth turning on");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null && action.equals(BluetoothDevice.ACTION_FOUND)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                boolean exist = false;

                //add available device to bluetooth service
                if (isBtServiceBound()) {
                    exist = getBtService().addAvailableDevice(device);
                }

                //add available device to available devices section
                if(!exist){
                    addAvailableDevice(getView(), device);
                }

            }

        }
    };

    private final BroadcastReceiver btMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getContext(), intent.getStringExtra(BluetoothService.EXTRA_MESSAGE), Toast.LENGTH_SHORT).show();
        }
    };

    private final BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int bluetoothState = intent.getIntExtra(BluetoothService.EXTRA_STATE, BluetoothService.STATE_ERROR);
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothService.EXTRA_DEVICE);
            switch (bluetoothState){
                case BluetoothService.STATE_CONNECTED:
                    setVisibilitiesWithBluetoothState(getView(), BluetoothAdapter.STATE_CONNECTED);
                    Log.d(TAG, "onReceive: " + device.getName() + " connected");
                    //test
                    Toast.makeText(getContext(), device.getName() + ": " + device.getAddress() + " connected", Toast.LENGTH_LONG).show();
                    break;
                case BluetoothService.STATE_DISCONNECTED:
                    setVisibilitiesWithBluetoothState(getView(), BluetoothAdapter.STATE_DISCONNECTED);
                    Log.d(TAG, "onReceive: " + device.getName() + " disconnected");
                    //test
                    Toast.makeText(getContext(), device.getName() + ": " + device.getAddress() + " disconnected", Toast.LENGTH_LONG).show();
                    break;
                case BluetoothService.STATE_RECONNECTING:
                    //test
                    Log.d(TAG, "onReceive: " + device.getName() + " reconnecting");
                    Toast.makeText(getContext(), device.getName() + ": " + device.getAddress() + " reconnecting", Toast.LENGTH_LONG).show();
                    break;
                default:
                    Log.d(TAG, "onReceive: bluetooth connection change: ERROR");
            }
        }
    };

    private final BroadcastReceiver deviceBoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                setPairedDevices(getView());
                String name;
                if(device.getName() != null){
                    name = device.getName();
                }
                else{
                    name = device.getAddress();
                }
                removeAvailableDevices(getView(), name);
            }
        }
    };

    /**
     * Initialization-------------------------------------------------------------------------------
     */
    public void initPairedDevicesSection(View view){
        //clear paired device section
        clearPairedDevices(view);

        //hide testing section
        LinearLayout bluetoothTestingSection = view.findViewById(R.id.bluetooth_testing_section);
        bluetoothTestingSection.setVisibility(View.GONE);

        Button showMoreBtn = view.findViewById(R.id.paired_devices_showmore_button);
        showMoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View pairedDevicesSection = (View)view.getParent();
                LinearLayout pairedDevicesContainer = pairedDevicesSection.findViewById(R.id.paired_device_container);
                int childCount = pairedDevicesContainer.getChildCount();
                for(int i=3; i<childCount; i++){
                    int visibility;
                    if(pairedDevicesContainer.getChildAt(i).getVisibility() == View.GONE){
                        visibility = View.VISIBLE;
                    }
                    else{
                        visibility = View.GONE;
                    }
                    pairedDevicesContainer.getChildAt(i).setVisibility(visibility);
                }
            }
        });
    }

    public void initBluetoothSwitch(View view){
        bluetoothSwitch = view.findViewById(R.id.bluetooth_switch);

        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    onBluetoothSwitchOn();
                }
                else {
                    onBluetoothSwitchOff();
                }

            }
        });
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
        bluetoothTestingSection.setVisibility(View.GONE);

        ImageButton sendBluetoothTextButton = view.findViewById(R.id.bluetooth_testing_send_button);
        final EditText testingEditText = view.findViewById(R.id.testing_edittext);
        sendBluetoothTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String testingText = testingEditText.getText().toString();
                if(testingText.length() > 0){
                    //test
                    Toast.makeText(getContext(), testingText, Toast.LENGTH_SHORT).show();

                    if(isBtServiceBound()){
                        getBtService().write(testingText);
                    }
                }
            }
        });
    }

    public void initBluetoothAvailableDevicesSection(final View fragment){
        LinearLayout availableDevicesSection = fragment.findViewById(R.id.available_devices_section);

        availableDevicesSection.setVisibility(View.GONE);

        clearAvailableDevices(fragment);

        ImageButton discoveryButton = availableDevicesSection.findViewById(R.id.discover_button);
        discoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //clear available devices
                clearAvailableDevices(getView());
                //start searching for available devices
                if(isBtServiceBound()){
                    getBtService().startDiscovery();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Stop discovery after 15s
                            getBtService().cancelDiscovery();
                            //test
                            Toast.makeText(getContext(), "Search done", Toast.LENGTH_SHORT).show();
                        }
                    }, 15000);
                }
                //test
                Toast.makeText(getContext(), "searching", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Layout changes-------------------------------------------------------------------------------
     */
    //show or hide views related to bluetooth
    public void setVisibilitiesWithBluetoothState(View view, int state){
        LinearLayout[] linearLayouts = new LinearLayout[3];
        linearLayouts[0] = view.findViewById(R.id.paired_device_section);
        linearLayouts[1] = view.findViewById(R.id.available_devices_section);
        linearLayouts[2] = view.findViewById(R.id.bluetooth_testing_section);

        for(int j=0; j<linearLayouts.length; j++){
            linearLayouts[j].setVisibility(View.GONE);
        }

        int i;
        switch (state){
            case BluetoothAdapter.STATE_CONNECTED:
                i = 3;
                break;

            case BluetoothAdapter.STATE_DISCONNECTED:
                i = 2;
                break;

            case BluetoothAdapter.STATE_ON:
                i = 2;
                break;

            default:
                i = 0;
                break;
        }

        for(int j=0; j<i; j++){
            if(linearLayouts[j] != null) {
                linearLayouts[j].setVisibility(View.VISIBLE);
                //test
                Log.d(TAG, "setVisibilitiesWithBluetoothState: set " + linearLayouts[j].getId() + "visible");
            }
        }
    }

    //set paired device to paired device section
    public void setPairedDevices(View view){
        //clear paired device container
        clearPairedDevices(view);

        //set device elements
        if(isBtServiceBound()){
            Set<BluetoothDevice> pairedDevices;
            pairedDevices = getBtService().getPairedDevices();
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                //add paired device
                addPairedDevice(view, device);
                //test
                Log.d("debug", "paired device: " + deviceName);

            }
        }
    }

    public void addPairedDevice(View view, BluetoothDevice device){
        String deviceName = device.getName();
        String deviceAddress = device.getAddress();

        LinearLayout pairedDeviceSection = view.findViewById(R.id.paired_device_section);
        LinearLayout pairedDeviceContainer = pairedDeviceSection.findViewById(R.id.paired_device_container);

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(LAYOUT_INFLATER_SERVICE);
        View pairedDeviceElement = inflater.inflate(R.layout.device_element, pairedDeviceContainer, false);

        //set device name for paired device element
        TextView pairedDeviceNameTextView = pairedDeviceElement.findViewById(R.id.device_element_name);
        if(deviceName != null && deviceName.length()>0){
            pairedDeviceNameTextView.setText(deviceName);
        }
        else{
            pairedDeviceNameTextView.setText(deviceAddress);
        }

        pairedDeviceElement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView pairedDeviceNameTextView = view.findViewById(R.id.device_element_name);
                if(isBtServiceBound()){
                    getBtService().startBtConnection(pairedDeviceNameTextView.getText().toString());
                }
                //test
                Toast.makeText(getContext(), pairedDeviceNameTextView.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        });

        pairedDeviceContainer.addView(pairedDeviceElement);

    }

    //clear paired device from paired device section
    public void clearPairedDevices(View view){
        LinearLayout pairedDeviceSection = view.findViewById(R.id.paired_device_section);
        LinearLayout pairedDeviceContainer = pairedDeviceSection.findViewById(R.id.paired_device_container);
        pairedDeviceContainer.removeAllViews();
    }

    public void setFocusableForDeviceName(View view, boolean isFocusable){
        EditText deviceNameView = view.findViewById(R.id.device_name_edittext);
        deviceNameView.setFocusableInTouchMode(isFocusable);
        deviceNameView.setFocusable(isFocusable);
    }

    public void clearAvailableDevices(View view){
        LinearLayout availableDevicesSection = view.findViewById(R.id.available_devices_section);
        LinearLayout availableDevicesContainer = availableDevicesSection.findViewById(R.id.available_devices_container);
        availableDevicesContainer.removeAllViews();
    }

    public void removeAvailableDevices(View view, String name){
        LinearLayout availableDevicesSection = view.findViewById(R.id.available_devices_section);
        LinearLayout availableDevicesContainer = availableDevicesSection.findViewById(R.id.available_devices_container);
        for(int i=0; i< availableDevicesContainer.getChildCount(); i++){
            if(((TextView)availableDevicesContainer.getChildAt(i).findViewById(R.id.device_element_name)).getText().equals(name)){
                availableDevicesContainer.removeViewAt(i);
                return;
            }
        }
    }

    public void addAvailableDevice(View view, BluetoothDevice device){
        String deviceName = device.getName();
        String deviceAddress = device.getAddress();

        LinearLayout availableDevicesSection = view.findViewById(R.id.available_devices_section);
        LinearLayout availableDevicesContainer = availableDevicesSection.findViewById(R.id.available_devices_container);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(LAYOUT_INFLATER_SERVICE);
        View availableDevice = inflater.inflate(R.layout.device_element, availableDevicesContainer, false);
        TextView availableDeviceName = availableDevice.findViewById(R.id.device_element_name);
        if(deviceName != null && deviceName.length() > 0){
            availableDeviceName.setText(deviceName);
        }
        else{
            availableDeviceName.setText(deviceAddress);
        }

        availableDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView availableDeviceName = view.findViewById(R.id.device_element_name);
                if(isBtServiceBound()){
                    getBtService().startBtPairing(availableDeviceName.getText().toString());
                }
                //test
                Toast.makeText(getContext(), availableDeviceName.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        });

        availableDevicesContainer.addView(availableDevice);
    }

    private BluetoothService getBtService(){
        return ((MainActivity)getActivity()).getBtService();
    }

    private boolean isBtServiceBound(){
        return (getBtService() != null);
    }

}
