package ntu.mdp.grp18;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

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
    Set<BluetoothDevice> pairedDevices = new ArraySet<>();
    Set<BluetoothDevice> availableDevices = new ArraySet<>();
    public static final java.util.UUID UUID = java.util.UUID.fromString("4c0b4d71-7036-4061-b1c0-e6562d896045");
    public static final String DEVICE_NAME = "appName";

    private static ConnectThread connectThread;
    private static AcceptThread acceptThread;
    private static ConnectedThread connectedThread;

    public static final int REQUEST_ENABLE_BT = 1;

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
        }
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    public synchronized void startClient(String name){

        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }

        for(BluetoothDevice device : pairedDevices){
            if((device.getName() != null && device.getName().equals(name)) || (device.getName() == null && device.getAddress().equals(name))){
                connectThread = new ConnectThread(device);
                connectThread.start();
                return;
            }
        }

        for(BluetoothDevice device : availableDevices){
            if((device.getName() != null && device.getName().equals(name)) || (device.getName() == null && device.getAddress().equals(name))){
                connectThread = new ConnectThread(device);
                connectThread.start();
                return;
            }
        }

    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(DEVICE_NAME, UUID);
            } catch (IOException e) {
                Log.e("debug", "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    //test
                    Log.d(TAG, "RFCOM server socket start......");
                    socket = mmServerSocket.accept();
                    //test
                    Log.d(TAG, "RFCOM server accepted connection");
                } catch (IOException e) {
                    Log.e("debug", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                        //test
                        Log.d("debug", "server socket closed");
                    } catch (IOException e) {
                        Log.e("debug", "Could not close the connect socket", e);
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
                Log.e("debug", "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(UUID);
            } catch (IOException e) {
                Log.e("debug", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                //test
                Log.d(TAG, "Run connect socket......");
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                //test
                Log.d(TAG, "Connect socket connected");
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    //test
                    Log.d(TAG, "Connect socket closed");

                } catch (IOException closeException) {
                    Log.e("debug", "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("debug", "Could not close the client socket", e);
            }
        }
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void manageConnectedSocket(BluetoothSocket socket){
        Log.d(TAG, "manageConnectedSocket: Starting");

        //Start the connected thread
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    public void write(byte[] out){
        Log.d(TAG, "write: Write called");
        connectedThread.write(out);
    }

}