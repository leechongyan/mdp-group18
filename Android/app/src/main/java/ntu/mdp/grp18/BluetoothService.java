package ntu.mdp.grp18;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.ArraySet;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;


public class BluetoothService extends Service {
    final String TAG = "BluetoothService";

    // Binder given to clients
    private final IBinder binder = new BluetoothBinder();

    // Bluetooth properties
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothDevice connectedDevice;
    Set<BluetoothDevice> pairedDevices = new ArraySet<>();
    Set<BluetoothDevice> availableDevices = new ArraySet<>();

    public static final UUID GENERAL_UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String DEVICE_NAME = "Grp18 tablet";

    private static ConnectThread connectThread;
    private static AcceptThread acceptThread;
    private static ConnectedThread connectedThread;

    public static final int REQUEST_ENABLE_BT = 1;

    private static int bluetoothState;
    public static final int STATE_CONNECTED = 1;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_RECONNECTING = 3;
    public static final int STATE_ERROR = -1;

    public static final String ACTION_BLUETOOTH_CONNECTION_CHANGED = "mdp.grp18.bluetooth.action.CONNECTION_CHANGED";
    public static final String ACTION_BLUETOOTH_MESSAGE_RECEIVED = "mdp.grp18.bluetooth.action.MESSAGE_RECEIVED";

    public static final String EXTRA_STATE = "mdp_grp18_extra_state";
    public static final String EXTRA_DEVICE = "mdp_grp18_extra_device";
    public static final String EXTRA_MESSAGE = "mdp_grp18_extra_message";

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class BluetoothBinder extends Binder {
        BluetoothService getService() {
            // Return this instance of BluetoothService so clients can call public methods
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter connectionChangeFilter = new IntentFilter();
        connectionChangeFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        connectionChangeFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(deviceConnectionChangeReceiver, connectionChangeFilter);

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter stateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(stateChangeReceiver, stateChangeFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(deviceConnectionChangeReceiver);
        unregisterReceiver(stateChangeReceiver);
    }

    /** method for clients */
    public boolean isBluetoothOn(){
        return bluetoothAdapter.isEnabled();
    }

    public void startBluetooth(Fragment fragment){
        if(!isBluetoothOn()) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            fragment.startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
        }
    }

    public void stopBluetooth(){
        if(isBluetoothOn()){
            bluetoothAdapter.disable();
            setState(STATE_DISCONNECTED, connectedDevice);
        }
    }

    public Set<BluetoothDevice> getPairedDevices(){
        pairedDevices = bluetoothAdapter.getBondedDevices();
        return pairedDevices;
    }

    public void startDiscovery(){
        bluetoothAdapter.startDiscovery();
        clearAvailableDevices();
    }

    public void cancelDiscovery(){
        bluetoothAdapter.cancelDiscovery();
    }

    private void clearAvailableDevices(){
        availableDevices.clear();
    }

    public boolean addAvailableDevice(BluetoothDevice device){
        if(availableDevices.contains(device)){
            return true;
        }
        availableDevices.add(device);
        return false;
    }

    private synchronized void startServer(){
        if(acceptThread != null){
            acceptThread.cancel();
            acceptThread = null;
        }

        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    public synchronized void startBtConnection(String name){
        cancelDiscovery();
        Log.d(TAG, "startConnecting: start connecting");
        for(BluetoothDevice device : pairedDevices){
            if((device.getName() != null && device.getName().equals(name)) || (device.getName() == null && device.getAddress().equals(name))){
                startClient(device);
                return;
            }
        }
    }

    private synchronized void startClient(BluetoothDevice device){

        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        stopBtConnection();

        //start connection
        connectThread = new ConnectThread(device);
        connectThread.start();

    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // GENERAL_UUID is the general bluetooth connection UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(DEVICE_NAME, GENERAL_UUID);

            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    Log.d(TAG, "run: A connection was accepted");
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Could not close the server socket", e);
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the server socket", e);
            }
        }
    }


    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // GENERAL_UUID is the general bluetooth UUID string, also used in the server code.
                tmp = device.createInsecureRfcommSocketToServiceRecord(GENERAL_UUID);
                Log.d(TAG, "ConnectThread: Create connect socket with device: " + device.getName());
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {

            waitForDisconnection();

            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            String strState = bluetoothStateToString(bluetoothState);
            Log.d(TAG, "run: client stop blocking due to state changing to " + strState);

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                Log.d(TAG, "run: Trying to connect to " + mmDevice.getName());
                mmSocket.connect();
                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                manageConnectedSocket(mmSocket);
            } catch (IOException connectException) {
                try {
                    Log.d(TAG,"trying fallback...");

                    mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice,1);
                    mmSocket.connect();

                    Log.d(TAG,"Connect thread connect() successful");
                    // The connection attempt succeeded. Perform work associated with
                    // the connection in a separate thread.
                    manageConnectedSocket(mmSocket);
                }
                catch (Exception e) {
                    Log.d(TAG, "Couldn't establish Bluetooth connection!");

                    if(isBluetoothSate(STATE_RECONNECTING)){
                        setState(STATE_DISCONNECTED, connectedDevice);
                    }

                    // Unable to connect; close the socket and return.
                    try {
                        mmSocket.close();
                    } catch (IOException closeException) {
                        Log.d(TAG, "Could not close the client socket", closeException);
                    }
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() { }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            //test
            Log.d(TAG, "ConnectedThread: Starting");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];

            int bytes;

            setState(STATE_CONNECTED, mmSocket.getRemoteDevice());

            //keep listening for input
            while(true){
                //read from InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);
                    //broadcastMessage
                    broadcastBtMessage(incomingMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream:" + text);

            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d(TAG, "Error writing to output stream: " + text);
            }
        }

        public void cancel(){
            try {
                mmSocket.close();
                Log.d(TAG, "cancel: close connected socket");
                if(!isBluetoothSate(STATE_RECONNECTING)){
                    setState(STATE_DISCONNECTED, null);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket){
        Log.d(TAG, "manageConnectedSocket: Starting");

        //Start the connected thread
        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }
        if(acceptThread != null){
            acceptThread.cancel();
            acceptThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    public void write(String out){
        Log.d(TAG, "write: Write called");
        connectedThread.write(out.getBytes());
    }

    public void startBtPairing(String name){
        for(BluetoothDevice device : availableDevices){
            if((device.getName() != null && device.getName().equals(name)) || (device.getName() == null && device.getAddress().equals(name))){
                //pair
                device.createBond();
                return;
            }
        }
    }

    private synchronized void setState(int state, BluetoothDevice device){
        String strState = bluetoothStateToString(state);
        Log.d(TAG, "setState: set state to " + strState);
        bluetoothState = state;

        notifyAll();

        BluetoothDevice previousDevice = setConnectedDevice(device);

        if(state == STATE_CONNECTED){
            broadcastState(bluetoothState, connectedDevice);
        }
        else{
            broadcastState(bluetoothState, previousDevice);
        }
    }

    private void broadcastState(int state, BluetoothDevice device){
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_CONNECTION_CHANGED);
        intent.putExtra(EXTRA_STATE, state);
        intent.putExtra(EXTRA_DEVICE, device);
        sendBroadcast(intent);
        Log.d(TAG, "broadcastState: sending...... ");
    }

    private void broadcastBtMessage(String message){
        Intent intent = new Intent();
        intent.setAction(ACTION_BLUETOOTH_MESSAGE_RECEIVED);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_DEVICE, getConnectedDevice());
        sendBroadcast(intent);
        Log.d(TAG, "broadcastBtMessage: sending......");
    }

    public synchronized BluetoothDevice setConnectedDevice(BluetoothDevice connectedDevice) {
        BluetoothDevice previous = this.connectedDevice;
        this.connectedDevice = connectedDevice;
        return previous;
    }

    private final BroadcastReceiver deviceConnectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                //try to reconnect
                String name = device.getName();
                if(name == null){
                    name = device.getAddress();
                }
                //try to reconnect if not disconnected
                if(!isBluetoothSate(STATE_DISCONNECTED)){
                    setState(STATE_RECONNECTING, device);
                    startBtConnection(name);
                }
            }
        }
    };

    private synchronized boolean isBluetoothSate(int state) {
        return bluetoothState == state;
    }

    private synchronized boolean waitForDisconnection(){
        while(isBluetoothSate(STATE_CONNECTED)){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void stopBtConnection(){
        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }

        startServer();
    }

    public String bluetoothStateToString(int state){
        String strState = "null";
        switch (state){
            case STATE_CONNECTED:
                strState = "STATE_CONNECTED";
                break;
            case STATE_DISCONNECTED:
                strState = "STATE_DISCONNECTED";
                break;
            case STATE_RECONNECTING:
                strState = "STATE_RECONNECTING";
                break;
        }
        return strState;
    }

    public synchronized int getBluetoothState(){
        return bluetoothState;
    }

    public synchronized  BluetoothDevice getConnectedDevice(){
        return connectedDevice;
    }

    private final BroadcastReceiver stateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        onBluetoothOn();
                        break;
                }
            }
        }
    };

    private void onBluetoothOn(){
        startServer();
    }
}