package ntu.mdp.grp18;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import ntu.mdp.grp18.fragments.BluetoothFragment;
import ntu.mdp.grp18.fragments.ControlFragment;
import ntu.mdp.grp18.fragments.OnBluetoothServiceBoundListener;
import ntu.mdp.grp18.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    BluetoothService btService;
    boolean btServiceBound = false;

    int currentPage;
    final int BLUETOOTH_PAGE = 0;
    final int SETTING_PAGE = 1;
    final int CONTROL_PAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton bluetoothPageBtn = findViewById(R.id.bluetooth_page_bottom_nav_btn);
        ImageButton settingPageBtn = findViewById(R.id.setting_page_bottom_nav_btn);
        ImageButton mapPageBtn = findViewById(R.id.map_page_bottom_nav_btn);

        bluetoothPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentPage == BLUETOOTH_PAGE){
                    return;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new BluetoothFragment()).commit();
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
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ControlFragment()).commit();
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
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment()).commit();
                setPageTitle("SETTINGS");
                setCurrentPage(SETTING_PAGE);
            }
        });

        //Create default fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ControlFragment()).commit();
        setPageTitle("CONTROL");
        setCurrentPage(CONTROL_PAGE);
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
}
