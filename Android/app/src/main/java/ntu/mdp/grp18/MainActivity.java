package ntu.mdp.grp18;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.view.MenuItem;

import ntu.mdp.grp18.fragments.BluetoothFragment;

public class MainActivity extends AppCompatActivity {

    BluetoothService btService;
    boolean btServiceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
//        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
//            @Override
//            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
//                Fragment selectedFragment;
//
//                switch (menuItem.getItemId()){
//                    case R.id.icon1:
//                        selectedFragment = new BluetoothFragment();
//                        break;
//                    default:
//                        //TODO:Complete the switch statement
//                        selectedFragment = new BluetoothFragment();
//                }
//
//                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
//
//                return true;
//            }
//        });

        //Create default fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new BluetoothFragment()).commit();

    }

    @Override
    protected void onStart() {
        super.onStart();
        //Bind to and start bluetoothService when main activity starts
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
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
            if(fragment instanceof BluetoothFragment){
                ((BluetoothFragment) fragment).onBluetoothServiceBound();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            btServiceBound = false;
            //tell fragment bluetooth service is unbound
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if(fragment instanceof BluetoothFragment){
                ((BluetoothFragment) fragment).onBluetoothServiceUnbound();
            }
        }
    };

    public BluetoothService getBtService(){
        return btService;
    }

    public boolean isBtServiceBound() {
        return btServiceBound;
    }
}
