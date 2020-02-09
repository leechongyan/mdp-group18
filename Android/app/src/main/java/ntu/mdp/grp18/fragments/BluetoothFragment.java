package ntu.mdp.grp18.fragments;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import ntu.mdp.grp18.BluetoothManager;
import ntu.mdp.grp18.DeviceListAdapter;
import ntu.mdp.grp18.R;

public class BluetoothFragment extends Fragment {
    private static final String TAG = "BluetoothFrag";
    private String connStatus;
    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mNewBTDevices;
    public ArrayList<BluetoothDevice> mPairedBTDevices;
    public DeviceListAdapter mNewDevlceListAdapter;
    public DeviceListAdapter mPairedDevlceListAdapter;
    TextView connStatusTextView;
    EditText editDeviceName;
    ListView otherDevicesListView;
    ListView pairedDevicesListView;
    Button connectBtn;
    EditText testDialog;
    ImageButton btnSend;
    ImageButton toggleBtn;
    TextView incomingMessages;
    ProgressDialog myDialog;
    Switch bluetoothSwitch;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    //for bluetooth
    BluetoothManager mBluetoothConnection;
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //same UUID found in BluetoothConnectionService class
    BluetoothDevice mBTDevice;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bluetooth_fragment, container, false);
        testDialog = (EditText) v.findViewById(R.id.testing_edittext);
        btnSend = (ImageButton) v.findViewById(R.id.bluetooth_testing_send_button);
        incomingMessages = (TextView) v.findViewById(R.id.testing_returntext);
        otherDevicesListView = (ListView) v.findViewById(R.id.otherDevicesListView);
        pairedDevicesListView = (ListView) v.findViewById(R.id.pairedDevicesListView);
        mNewBTDevices = new ArrayList<>();
        mPairedBTDevices = new ArrayList<>();
        bluetoothSwitch = (Switch) v.findViewById(R.id.bluetooth_switch);
        toggleBtn = (ImageButton) v.findViewById(R.id.toggleButtonScan);
        connectBtn = (Button) v.findViewById(R.id.connectBtn);
        editDeviceName = (EditText) v.findViewById(R.id.device_name_edittext);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        //Broadcasts when bond state changes (Pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(mBroadcastReceiver4, filter);

        //Broadcasts when bluetooth state changes (connected, disconnected etc) custom receiver
        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver5, filter2);

        return v;

    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initBluetoothDeviceNameSection(view);
        //If bluetooth is already on, Set the toggle to true when pop up opens
        if(mBluetoothAdapter.isEnabled()){
            bluetoothSwitch.setChecked(true);
            setVisibilitiesWithBluetoothState(getView(),true);
            bluetoothSwitch.setText("ON");
        }else{
            setVisibilitiesWithBluetoothState(getView(),false);
            bluetoothSwitch.setText("OFF");
        }

        //togglebutton
        toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleButtonScan(v);
            }
        });

        //nested scroll view handle
        pairedDevicesListView.setOnTouchListener(new ListView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        //nested scroll view handle
        otherDevicesListView.setOnTouchListener(new ListView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        //New Devices List View event handler
        otherDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //cancel discovery since it is memory intensive
                mBluetoothAdapter.cancelDiscovery();
                //this is to remove the highlight bar(if any) when a device is selected at the paired devices list view
                pairedDevicesListView.setAdapter(mPairedDevlceListAdapter);

                String deviceName = mNewBTDevices.get(i).getName();
                String deviceAddress = mNewBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);


                //create the bond for first time pairing devices
                //NOTE: createBond() method requires API 17+
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Log.d(TAG, "onItemClick: Initiating pairing with " + deviceName);
                    mNewBTDevices.get(i).createBond();

                    //start connection service AFTER bonding (acceptthread will start first and the device will sit and wait for a connection,
                    //which is the connectthread that is initiated with the connect button)
                    mBluetoothConnection = new BluetoothManager(getActivity());
                    mBTDevice = mNewBTDevices.get(i);
                }
            }
        });
        pairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //cancel discovery since it is memory intensive
                mBluetoothAdapter.cancelDiscovery();
                //this is to remove the highlight bar(if any) when a device is selected at the new devices list view
                otherDevicesListView.setAdapter(mNewDevlceListAdapter);

                String deviceName = mPairedBTDevices.get(i).getName();
                String deviceAddress = mPairedBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);

                //start connection service (acceptthread will start first and the device will sit and wait for a connection,
                // which is the connectthread that is initiated with the connect button)
                mBluetoothConnection = new BluetoothManager(getActivity());
                mBTDevice = mPairedBTDevices.get(i);
            }
        });

        //On off Switch button event handler
        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Log.d(TAG, "onChecked: Switch button toggled. Enabling/Disabling Bluetooth");
                //this is the most convenient way of changing the text for switch
                if(isChecked){
                    compoundButton.setText("ON");
                    setVisibilitiesWithBluetoothState(getView(),true);
                }else
                {
                    compoundButton.setText("OFF");
                    setVisibilitiesWithBluetoothState(getView(),false);
                    clear(getView());
                }

                if(mBluetoothAdapter ==null){
                    Log.d(TAG, "enableDisableBT: Device does not support Bluetooth capabilities!");
                    Toast.makeText(getActivity(), "Device Does Not Support Bluetooth capabilities!", Toast.LENGTH_LONG).show();
                    compoundButton.setChecked(false);
                }
                else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "enableDisableBT: enabling Bluetooth");
                        Log.d(TAG, "enableDisableBT: Making device discoverable for 600 seconds.");
                        //Enable Bluetooth - removed because ACTION_REQUEST_DISCOVERABLE can turn on bluetooth so this is redundant
                        /*Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivity(enableBTIntent);*/
                        //Enable discoverability
                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
                        startActivity(discoverableIntent);

                        compoundButton.setChecked(true);//need this for cases where the user tapped outside of the pop up box during allow/deny prompt

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        getActivity().registerReceiver(mBroadcastReceiver1, BTIntent); //intercepts changes to bluetooth status and logs them

                        IntentFilter discoverIntent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                        getActivity().registerReceiver(mBroadcastReceiver2, discoverIntent); //intercepts changes to discoverability status and logs them
                    }
                    if (mBluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "enableDisableBT: disabling Bluetooth");
                        mBluetoothAdapter.disable();

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        getActivity().registerReceiver(mBroadcastReceiver1, BTIntent); //intercepts changes to bluetooth status and logs them
                    }

                }
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBTDevice ==null)
                {
                    Toast.makeText(getActivity(), "Please Select a Device before connecting.", Toast.LENGTH_LONG).show();
                }
                else {
                    startConnection();
                }
            }
        });


        //added these 3 lines
//        connStatusTextView = (TextView) findViewById(R.id.connStatusTextView);
//        connStatus ="Disconnected";
//        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
//        //
//        if (sharedPreferences.contains("connStatus"))
//            connStatus = sharedPreferences.getString("connStatus", "");
//
//        connStatusTextView.setText(connStatus);


        //Progress dialog to show when the bluetooth is disconnected
        myDialog = new ProgressDialog(getActivity());
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                byte[] bytes = testDialog.getText().toString().getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
                testDialog.setText("");
            }
        });
    }
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("receivedMessage");
            Log.d(TAG, "received: "+text);
            incomingMessages.setText(text);
        }
    };

    public void toggleButtonScan(View view){
        Log.d(TAG, "toggleButton: Scanning for unpaired devices.");
        mNewBTDevices.clear(); //clear list of bt devices whenever scan button is pressed so that list view doesnt display previously found devices
        mPairedBTDevices.clear();
        pairedDevicesListView.setAdapter(null);
        otherDevicesListView.setAdapter(null);
        if(mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(getActivity(), "Please turn on Bluetooth first!", Toast.LENGTH_SHORT).show();
            }
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "toggleButton: Cancelling Discovery.");

                //Check bluetooth permissions in manifest
                checkBTPermissions();

                mBluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND); //ACTION_FOUND: remote device discovered
                getActivity().registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
            } else if (!mBluetoothAdapter.isDiscovering()) {
                //Check bluetooth permissions in manifest
                checkBTPermissions();

                mBluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND); //ACTION_FOUND: remote device discovered
                getActivity().registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
            }
            //get a list of bonded devices. This does not require any discovery

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            Log.d(TAG, "toggleButton: Number of paired devices found: "+ pairedDevices.size());
            for(BluetoothDevice d : pairedDevices){
                Log.d(TAG, "Paired Devices: "+ d.getName() +" : " + d.getAddress());
                mPairedBTDevices.add(d);
                mPairedDevlceListAdapter = new DeviceListAdapter(getActivity(), R.layout.device_element, mPairedBTDevices);
                pairedDevicesListView.setAdapter(mPairedDevlceListAdapter); //set adapter to the list
            }
        }
    }

    /**
     * This method is required for all devices running API23+
     * Android must programatically check the permissions for Bluetooth. Putting the proper permissions in the manifest is not enough.
     * Note: This will only execute on versions > LOLLIPOP because it is not needed otherwise
     */
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck = getActivity().checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck += getActivity().checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            }
            if (permissionCheck != 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //any number
                }
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");

        }
    }
    // Create a BroadcastReceiver for turning on/off bluetooth
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");

                        break;
                    //BLUETOOTH TURNING ON STATE
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for Bluetooth Discoverability mode on/off or expiry
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast receiver for listing devices that are not yet paired
     * Executed by toggleButtonScan
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if(action.equals(BluetoothDevice.ACTION_FOUND)) {//ACTION_FOUND: remote device discovered
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); //get device object(parcelable)
                mNewBTDevices.add(device); //add remote device found to the arraylist
                Log.d(TAG, "onReceive: "+ device.getName() +" : " + device.getAddress());
                mNewDevlceListAdapter = new DeviceListAdapter(context, R.layout.device_element, mNewBTDevices);
                otherDevicesListView.setAdapter(mNewDevlceListAdapter); //set adapter to the list

            }
        }
    };

    /**
     * Broadcast receiver for bluetooth bonding changes
     * Only logging here to track the events, not other action needed
     */
    private BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases
                //case 1: when the device is already bonded
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BOND_BONDED.");
                    Toast.makeText(getActivity(), "Successfully paired with " + mDevice.getName(), Toast.LENGTH_SHORT).show();
                    mBTDevice = mDevice; //assign paired device to global variable
                }
                //case 2: Creating a new bond
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BOND_BONDING.");
                }
                //case 1: Breaking a bond
                if(mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BOND_NONE.");
                }
            }
        }
    };

    /**
     * Broadcast receiver for bluetooth connection status
     * NOTE: failed to connect to device will display a toast message which is found in BluetoothConnectionService ConnectThread
     */
    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //final String action = intent.getAction();
            //BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getActivity().getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();

            if(status.equals("connected")){
                //When the device reconnects, this broadcast will be called again to enter CONNECTED if statement
                //must dismiss the previous dialog that is waiting for connection if not it will block the execution
                try {
                    myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                Toast.makeText(getActivity(), "Device now connected to "+mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected");
                editor.putString("connectedDevice",mDevice.getName());
                //connStatusTextView.setText("Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected")){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected from "+mDevice.getName());
                Toast.makeText(getActivity(), "Disconnected from "+mDevice.getName(), Toast.LENGTH_LONG).show();
                //start accept thread and wait on the SAME device again
                mBluetoothConnection = new BluetoothManager(getActivity());
                mBluetoothConnection.startAcceptThread();

                // For displaying disconnected for all page
                sharedPreferences = getActivity().getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putString("connStatus", "Disconnected");
                editor.remove("connectedDevice");
                //TextView connStatusTextView = findViewById(R.id.connStatusTextView);
                //connStatusTextView.setText("Disconnected");

                //show disconnected dialog
                try {
                    myDialog.show();
                }catch (Exception e){
                    Log.d(TAG, "BluetoothPopUp: mBroadcastReceiver5 Dialog show failure");
                }
            }
            editor.commit();
        }
    };

    /**
     * method for starting connection(connectthread) after device is in acceptthread
     * NOTE: the connection will fail and app will crash if the devices did not pair first
     */
    public void startConnection(){
        startBTConnection(mBTDevice,myUUID);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection");

        mBluetoothConnection.startClientThread(device, uuid);
    }

    //show or hide views related to bluetooth
    public void setVisibilitiesWithBluetoothState(View view, boolean isBluetoothOn){
        LinearLayout[] linearLayouts = new LinearLayout[3];
        linearLayouts[0] = view.findViewById(R.id.paired_device_section);
        linearLayouts[1] = view.findViewById(R.id.bluetooth_testing_section);
        linearLayouts[2] = view.findViewById(R.id.available_devices_section);

        int visibility = isBluetoothOn ? View.VISIBLE : View.GONE;

        for(int i=0; i<linearLayouts.length; i++){
            if(linearLayouts[i] != null) {
                linearLayouts[i].setVisibility(visibility);
            }
        }
    }

    public void initBluetoothDeviceNameSection(View view) {

        //Populate the right device name retrieved from memory into editText view
        //Get the default phone name in case no cached name in app
        String deviceName = mBluetoothAdapter.getName();
        sharedPreferences = getActivity().getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        //Todo: [BE] get the deviceName from "shared preference"
        if (sharedPreferences.contains("deviceName")) {
            deviceName = sharedPreferences.getString("deviceName","");
        }

        editDeviceName.setText(deviceName);

        //Todo: [BT] change the device name of our tablet that is shown in other devices' bluetooth lists, to 'deviceName'

        editDeviceName.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //Clear focus here from editText
                    editDeviceName.clearFocus();

                    String deviceName = editDeviceName.getText().toString();

                    //test
                    Log.d("debug", deviceName);

                    /**Todo: [BT] change the device name of our tablet that is shown in other devices' bluetooth lists (if that affects the connection, then after that, try to reconnect with the current paired device if there's one)
                     * Todo: [BE] save the device name using "shared preference" so that it's remembered next time
                     **/
                    mBluetoothAdapter.setName(deviceName);
                    editor = sharedPreferences.edit();
                    editor.putString("deviceName",deviceName);
                    editor.commit();

                }

                InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                return false;
            }
        });
    }

    public void clear(View view){
        testDialog.setText("");
        incomingMessages.setText("");
        mNewBTDevices.clear();
        mPairedBTDevices.clear();
        otherDevicesListView.setAdapter(null);
        pairedDevicesListView.setAdapter(null);

    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: called");
        super.onDestroy();
        //close broadcast receivers when activity is finishing/destroyed
        try {
            getActivity().unregisterReceiver(mBroadcastReceiver1); //try catch for cases where bluetooth adapter =null on devices that dont support bluetooth
            getActivity().unregisterReceiver(mBroadcastReceiver2);
            getActivity().unregisterReceiver(mBroadcastReceiver3);
            getActivity().unregisterReceiver(mBroadcastReceiver4);
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver5);
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

}