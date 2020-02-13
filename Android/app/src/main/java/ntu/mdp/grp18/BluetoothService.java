package ntu.mdp.grp18;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;


public class BluetoothService extends Service {
    final String TAG = "BluetoothService";

    // Binder given to clients
    private final IBinder binder = new BluetoothBinder();

    // Bluetooth properties
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Set<BluetoothDevice> pairedDevices = new ArraySet<>();
    Set<BluetoothDevice> availableDevices = new ArraySet<>();
    public static final java.util.UUID MY_UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String DEVICE_NAME = "HUAWEI Mate 20 Pro";

    private static ConnectThread connectThread;
    private static AcceptThread acceptThread;
    private static ConnectedThread connectedThread;

    private static UUID targetUuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final int REQUEST_ENABLE_BT = 1;

    public static int bluetoothState;
    public static final int CONNECTED = 1;
    public static final int DISCONNECTED = 0;
    public static final int DISCONNECTING = 2;
    public static final int RECONNECTING = 3;
    public static final int RECONNECTED = 4;

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
    public void onCreate() {
        super.onCreate();

        IntentFilter connectionChangeFilter = new IntentFilter();
        connectionChangeFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        connectionChangeFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(connectionChangeReceiver, connectionChangeFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(connectionChangeReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /** method for clients */
    public boolean isBluetoothOn(){
        return bluetoothAdapter.isEnabled();
    }

    public void startBluetooth(Fragment fragment){
        if(!isBluetoothOn()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            fragment.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            bluetoothAdapter.enable();

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            fragment.startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
        }
    }

    public void stopBluetooth(){
        if(isBluetoothOn()){
            bluetoothAdapter.disable();
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

    public void clearAvailableDevices(){
        availableDevices.clear();
    }

    public boolean addAvailableDevice(BluetoothDevice device){
        if(availableDevices.contains(device)){
            return true;
        }
        availableDevices.add(device);
        return false;
    }

    public synchronized void startServer(){
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

    public synchronized void startConnecting(String name){
        cancelDiscovery();
        Log.d(TAG, "startConnecting: start connecting");
        for(BluetoothDevice device : pairedDevices){
            if((device.getName() != null && device.getName().equals(name)) || (device.getName() == null && device.getAddress().equals(name))){
                //request uuid of target device
                //requestUuidFromDevice(device);
                startClient(device);
                return;
            }
        }
    }

    public synchronized void startClient(BluetoothDevice device){

        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        stopConnection();

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
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(DEVICE_NAME, MY_UUID);

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
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
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
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

//    private class AcceptThread extends Thread {
//        private final BluetoothServerSocket mmServerSocket;
//
//        public AcceptThread() {
//            // Use a temporary object that is later assigned to mmServerSocket
//            // because mmServerSocket is final.
//            BluetoothServerSocket tmp = null;
//            try {
//                // UUID is the app's UUID string, also used by the client code.
//                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("ime", MY_UUID);
//            } catch (IOException e) {
//                Log.e(TAG, "Socket's listen() method failed", e);
//            }
//            mmServerSocket = tmp;
//        }
//
//        public void run() {
//            BluetoothSocket socket = null;
//            // Keep listening until exception occurs or a socket is returned.
//            while (true) {
//                try {
//                    //test
//                    Log.d(TAG, "RFCOM server socket start......");
//                    socket = mmServerSocket.accept();
//                    //test
//                    Log.d(TAG, "RFCOM server accepted connection");
//                } catch (IOException e) {
//                    Log.e("debug", "Socket's accept() method failed", e);
//                    break;
//                }
//
//                if (socket != null) {
//                    // A connection was accepted. Perform work associated with
//                    // the connection in a separate thread.
//                    manageConnectedSocket(socket);
//                    try {
//                        mmServerSocket.close();
//                        //test
//                        Log.d(TAG, "server socket closed");
//                    } catch (IOException e) {
//                        Log.e(TAG, "Could not close the connect server socket", e);
//                    }
//                    break;
//                }
//            }
//        }
//
//        // Closes the connect socket and causes the thread to finish.
//        public void cancel() {
//            try {
//                mmServerSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "Could not close the connect socket", e);
//            }
//        }
//    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createInsecureRfcommSocketToServiceRecord(targetUuid);
                Log.d(TAG, "ConnectThread: Create connect socket with device: " + device.getName());
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {

            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            waitForDisconnection();

            String strState = "null";
            switch (bluetoothState){
                case CONNECTED:
                    strState = "CONNECTED";
                    break;
                case DISCONNECTED:
                    strState = "DISCONNECTED";
                    break;
                case DISCONNECTING:
                    strState = "DISCONNECTING";
            }
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

                    if(isBluetoothSate(RECONNECTING)){
                        setState(DISCONNECTED);
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
        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "Could not close the client socket", e);
//            }
        }
    }

//    private class ConnectThread extends Thread {
//        private BluetoothSocket mmSocket;
//        private final BluetoothDevice mmDevice;
//
//        public ConnectThread(BluetoothDevice device) {
//            // Use a temporary object that is later assigned to mmSocket
//            // because mmSocket is final.
//            BluetoothSocket tmp = null;
//            mmDevice = device;
//
//            try {
//                // Get a BluetoothSocket to connect with the given BluetoothDevice.
//                // UUID is the app's UUID string, also used in the server code.
//                tmp = device.createInsecureRfcommSocketToServiceRecord(targetUuid);
////                tmp = device.createRfcommSocketToServiceRecord(targetUuid);
//            } catch (IOException e) {
//                Log.e(TAG, "Socket's create() method failed", e);
//            }
//            mmSocket = tmp;
//        }
//
//        public void run() {
//            // Cancel discovery because it otherwise slows down the connection.
//            bluetoothAdapter.cancelDiscovery();
//
//            try {
//                //test
//                Log.d(TAG, "Run connect socket......");
//                // Connect to the remote device through the socket. This call blocks
//                // until it succeeds or throws an exception.
//                mmSocket.connect();
//                //test
//                Log.d(TAG, "Connect socket connected");
//            } catch (IOException connectException) {
//                try {
//                    mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice,1);
//                    Log.d(TAG, "run: try fall back connect");
//                    mmSocket.connect();
//                } catch (Exception e) {
//                    Log.d(TAG, "run: fall back connect failed");
//                }
//                // Unable to connect; close the socket and return.
//                try {
//                    mmSocket.close();
//                    //test
//                    Log.d(TAG, "Connect socket closed with connectException");
//                    Log.e(TAG, "Connect socket exception: ", connectException);
//
//                } catch (IOException closeException) {
//                    Log.e(TAG, "Could not close the client socket", closeException);
//                }
//                return;
//            }
//
//            // The connection attempt succeeded. Perform work associated with
//            // the connection in a separate thread.
//            manageConnectedSocket(mmSocket);
//        }
//
//        // Closes the client socket and causes the thread to finish.
//        public void cancel() {
//            try {
//                mmSocket.close();
//                //test
//                Log.d(TAG, "Connect socket closed");
//            } catch (IOException e) {
//                Log.e(TAG, "Could not close the client socket", e);
//            }
//        }
//    }

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

            setState(CONNECTED);

            //keep listening for input
            while(true){
                //read from InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void manageConnectedSocket(BluetoothSocket socket){
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

    public void startPairing(String name){
        for(BluetoothDevice device : availableDevices){
            if((device.getName() != null && device.getName().equals(name)) || (device.getName() == null && device.getAddress().equals(name))){
                //pair
                device.createBond();
                return;
            }
        }
    }

    public synchronized void setState(int state){
        String strState = "null";
        switch (state){
            case CONNECTED:
                strState = "CONNECTED";
                break;
            case DISCONNECTED:
                strState = "DISCONNECTED";
                break;
            case DISCONNECTING:
                strState = "DISCONNECTING";
                break;
            case RECONNECTING:
                strState = "RECONNECTING";
                break;
            case RECONNECTED:
                strState = "RECONNECTED";
                break;
        }
        Log.d(TAG, "setState: set state to " + strState);
        bluetoothState = state;

        notifyAll();
    }

    private final BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {
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
                //try to reconnect if not disconnecting
                if(!isBluetoothSate(DISCONNECTING) && !isBluetoothSate(RECONNECTED)){
                    setState(RECONNECTING);
                    startConnecting(name);
                }
                else{
                    setState(DISCONNECTED);
                }
            }
            else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
                if(isBluetoothSate(RECONNECTING)){
                    setState(RECONNECTED);
                }
                else if(isBluetoothSate(CONNECTED)){
                    startClient(device);
                    startClient(device);
                }
            }
        }
    };

    private synchronized boolean isBluetoothSate(int state) {
        return bluetoothState == state;
    }

    private synchronized boolean waitForDisconnection(){
        while(!isBluetoothSate(DISCONNECTED) && !isBluetoothSate(RECONNECTING)){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void stopConnection(){
        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;

            if(!isBluetoothSate(RECONNECTING)){
                setState(DISCONNECTING);
            }
        }

        startServer();
    }


}