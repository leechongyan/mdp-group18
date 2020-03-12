package ntu.mdp.grp18;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.Image;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import ntu.mdp.grp18.fragments.BluetoothFragment;
import ntu.mdp.grp18.fragments.ControlFragment;
import ntu.mdp.grp18.fragments.DriveFragment;
import ntu.mdp.grp18.fragments.OnBluetoothServiceBoundListener;
import ntu.mdp.grp18.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";

    BluetoothService btService;
    boolean btServiceBound = false;

    Fragment bluetoothFragment, settingsFragment, controlFragment, driveFragment;

    int currentPage;
    final int BLUETOOTH_PAGE = 0;
    final int SETTING_PAGE = 1;
    final int CONTROL_PAGE = 2;
    final int DRIVE_PAGE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton bluetoothPageBtn = findViewById(R.id.bluetooth_page_bottom_nav_btn);
        ImageButton settingPageBtn = findViewById(R.id.setting_page_bottom_nav_btn);
        ImageButton mapPageBtn = findViewById(R.id.map_page_bottom_nav_btn);
        ImageButton drivePageBtn = findViewById(R.id.drive_page_bottom_nav_btn);

        bluetoothFragment = new BluetoothFragment();
        controlFragment = new ControlFragment();
        settingsFragment = new SettingsFragment();
        driveFragment = new DriveFragment();

        bluetoothPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentPage == BLUETOOTH_PAGE){
                    return;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, bluetoothFragment).commit();
                setPageTitle("BLUETOOTH");
                setCurrentPage(BLUETOOTH_PAGE);
            }
        });

        mapPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentPage == CONTROL_PAGE){
                    return;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, controlFragment).commit();
                setPageTitle("CONTROL");
                setCurrentPage(CONTROL_PAGE);
            }
        });

        settingPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentPage == SETTING_PAGE){
                    return;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, settingsFragment).commit();
                setPageTitle("SETTINGS");
                setCurrentPage(SETTING_PAGE);
            }
        });

        drivePageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentPage == DRIVE_PAGE){
                    return;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, driveFragment).commit();
                setPageTitle("DRIVE");
                setCurrentPage(DRIVE_PAGE);
            }
        });

        //Create default fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, controlFragment).commit();
        setPageTitle("CONTROL");
        setCurrentPage(CONTROL_PAGE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Bind to and start bluetoothService when main activity starts
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        IntentFilter connectionChangeFilter = new IntentFilter(BluetoothService.ACTION_BLUETOOTH_CONNECTION_CHANGED);
        registerReceiver(connectionChangeReceiver, connectionChangeFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(connectionChangeReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to BluetoothService, cast the IBinder and get BluetoothService instance
            BluetoothService.BluetoothBinder binder = (BluetoothService.BluetoothBinder) service;
            btService = binder.getService();
            btServiceBound = true;

            //test
            Log.d("debug", ""+btService.isBluetoothOn());

            //tell fragment bluetooth service is bound
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if(fragment instanceof OnBluetoothServiceBoundListener){
                ((OnBluetoothServiceBoundListener) fragment).onBluetoothServiceBound();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            btServiceBound = false;
//            //tell fragment bluetooth service is unbound
//            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
//            if(fragment instanceof OnBluetoothServiceBoundListener){
//                ((OnBluetoothServiceBoundListener) fragment).onBluetoothServiceUnbound();
//            }
        }
    };

    public BluetoothService getBtService(){
        return btService;
    }

    public boolean isBtServiceBound() {
        return btServiceBound;
    }

    private void setPageTitle(String title){
        TextView pageTitleTextView = findViewById(R.id.page_title);
        pageTitleTextView.setText(title);
    }

    private void setCurrentPage(int currentPage){
        this.currentPage = currentPage;
    }

    private final BroadcastReceiver connectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int bluetoothState = intent.getIntExtra(BluetoothService.EXTRA_STATE, BluetoothService.STATE_ERROR);
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothService.EXTRA_DEVICE);
            switch (bluetoothState){
                case BluetoothService.STATE_CONNECTED:
                    Log.d(TAG, "onReceive: " + device.getName() + " connected");
                    Toast.makeText(getApplicationContext(), device.getName() + ": " + device.getAddress() + " connected", Toast.LENGTH_LONG).show();
                    break;
                case BluetoothService.STATE_DISCONNECTED:
                    Log.d(TAG, "onReceive: " + device.getName() + " disconnected");
                    //test
                    Toast.makeText(getApplicationContext(), device.getName() + ": " + device.getAddress() + " disconnected", Toast.LENGTH_LONG).show();
                    break;
                case BluetoothService.STATE_RECONNECTING:
                    //test
                    Log.d(TAG, "onReceive: " + device.getName() + " reconnecting");
                    Toast.makeText(getApplicationContext(), device.getName() + ": " + device.getAddress() + " reconnecting", Toast.LENGTH_LONG).show();
                    break;
                default:
                    Log.d(TAG, "onReceive: bluetooth connection change: ERROR");
            }
        }
    };
}
