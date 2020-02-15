package ntu.mdp.grp18;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.*;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class BluetoothManager {

    private static final String TAG = "BluetoothMgr";
    private static final String APPNAME = "MDP";
    private static final UUID  MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mBluetoothAdaptor;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    private static boolean mBluetoothStatusConnected = false;
    private ConnectedThread mConnectedThread;

    public BluetoothManager(Context mContext) {
        this.mBluetoothAdaptor = BluetoothAdapter.getDefaultAdapter();
        this.mContext = mContext;
        startAcceptThread();
    }

    /**
     * This thread runs while listening for incoming connections
     * runs till connection is accepted or cancelled
     */
    private class AcceptThread extends Thread {
        // local server socket
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            // create a new listening server socket
            try{
                tmp = mBluetoothAdaptor.listenUsingInsecureRfcommWithServiceRecord(APPNAME,MY_UUID_INSECURE);

                Log.d(TAG, "AcceptThread: Setting up Server using: "+MY_UUID_INSECURE);
            }catch(IOException e){
                Log.e(TAG, "AcceptThread: IOException: "+e.getMessage());
            }
            mServerSocket = tmp;
        }

        public void run(){
            Log.d(TAG, "run: AcceptThread Running");

            BluetoothSocket socket = null;

            try {
                //This is a blocking call and will only return on a successful connection or exception
                Log.d(TAG, "run: RFCOM server socket start......");

                socket = mServerSocket.accept();

                Log.d(TAG, "run: RFCOM server socket accepted connection");
            }catch(IOException e){
                Log.e(TAG, "AcceptThread: IOException: "+e.getMessage());
            }

            if(socket!=null){
                connected(socket,mDevice);
            }

            Log.i(TAG,"END mAcceptThread");
        }

        public void cancel(){
            Log.d(TAG,"cancel: Cancelling AcceptThread");
            try{
                mServerSocket.close();
            }catch(IOException e){
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed: "+e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread{
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG,"ConnectThread: started");
            mDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG,"RUN mConnectThread");

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                Log.d(TAG,"ConnectThread: Trying to create InsecureRFcomSocket using UUID: "+MY_UUID_INSECURE);
                tmp = mDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcomSocket: "+e.getMessage());
            }

            mSocket = tmp;

            // Cancel discovery
            mBluetoothAdaptor.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                //Make a connection to the BluetoothSocket
                //This is a blocking call and will only return upon successful connection or exception
                mSocket.connect();

                Log.d(TAG, "RUN: ConnectThread connected.");

                connected(mSocket,mDevice);

            } catch (IOException e) {

                try {
                    Log.e("", "trying fallback...");

                    mSocket = (BluetoothSocket) mDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mDevice, 1);
                    mSocket.connect();
                    connected(mSocket, mDevice);
                    Log.e("", "Connected");
                } catch (Exception e2) {
                    Log.e("", "Couldn't establish Bluetooth connection!");
                    try {
                        mSocket.close();
                        Log.d(TAG, "RUN: ConnectThread socket closed.");
                    } catch (IOException e1) {
                        Log.e(TAG, "RUN: ConnectThread: Unable to close connection in socket." + e1.getMessage());
                    }
                    Log.d(TAG, "RUN: ConnectThread: could not connect to UUID." + MY_UUID_INSECURE);
                    System.out.println("errorx: " + e2.getMessage());
                }
            }

            connected(mSocket, mDevice);
        }

        public void cancel(){
            try{
                Log.d(TAG,"cancel: Closing Client Socket");
                mSocket.close();
            }catch(IOException e){
                Log.e(TAG, "cancel: close() of mSocket in ConnectThread failed: "+e.getMessage());
            }
        }
    }

    /**
     * Start the chat service
     * Start AcceptThread to begin a session in listening mode
     * Called by the Activity onResume()
     */
    public synchronized void startAcceptThread(){
        Log.d(TAG, "Start");

        //Cancel any thread attempting to make a connection
        if(mConnectThread!=null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mInsecureAcceptThread==null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    /**
     * AcceptThread starts and sits waiting for a connection
     * ConnectThread starts and attempts to make a connection with the other devices AcceptThread
     * @param device
     * @param uuid
     */
    public void startClientThread(BluetoothDevice device, UUID uuid){
        Log.d(TAG,"startClient: Started");

        //initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please Wait...", true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG,"ConnectedThread: Starting");

            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //now notifying all the activities that bluetooth is connected
            mBluetoothStatusConnected = true;
            Intent connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status","Connected!");
            connectionStatus.putExtra("Device",mDevice);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);

            // dismiss the progressdialog when connection is established
            try{
                mProgressDialog.dismiss();
            }catch(NullPointerException e){
                e.printStackTrace();
            }

            try{
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
                Log.d(TAG,"ConnectedThread: InputStream and OutputStream configured");
            }catch(IOException e){
                e.printStackTrace();
                Log.d(TAG,"ConnectedThread: In and Out failed");
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run(){
            // buffer store for the stream
            byte[] buffer = new byte[1024];

            //bytes returned from read()
            int bytes;

            while(true){
                //Read from the InputStream
                try{
                    bytes = mInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG,"InputStream: "+incomingMessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("receivedMessage",incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                }catch(IOException e){
                    Log.e(TAG,"run: Error reading inputStream: "+e.getMessage());

                    // notify disconnection
                    mBluetoothStatusConnected = false;
                    Intent connectionStatus = new Intent("ConnectionStatus");
                    connectionStatus.putExtra("Status","Disconnected!");
                    connectionStatus.putExtra("Device",mDevice);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);

                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG,"write: Writing to outputstream: "+text);
            try{
                mOutStream.write(bytes);
            }catch(IOException e){
                Log.e(TAG,"write: Error writing to outputstream: "+e.getMessage());
            }
        }
        // Call this from the main activity to shutdown the connection
        public void cancel(){
            try{
                mSocket.close();
            }catch(IOException e){
                Log.e(TAG, "cancel: close() of mSocket in ConnectedThread failed: "+e.getMessage());
            }
        }
    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice mDevice){
        Log.d(TAG, "connected: Starting");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out){
        //synchronise a copy of the ConnectedThread
        Log.d(TAG,"write: Write Called");
        mConnectedThread.write(out);
    }
}
