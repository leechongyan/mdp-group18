package ntu.mdp.grp18;

import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

import ntu.mdp.grp18.fragments.BluetoothFragment;
import ntu.mdp.grp18.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.setting_fragment, false);
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

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment()).commit();

    }

}
