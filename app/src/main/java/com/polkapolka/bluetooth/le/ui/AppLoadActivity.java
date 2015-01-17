package com.polkapolka.bluetooth.le.ui;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Handler;
import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.app.SherlockActivity;
import com.polkapolka.bluetooth.le.R;

/**
 * Created by evilisn_jiang on 2015/1/15.
 */
public class AppLoadActivity extends SherlockActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mHeaderDataField;
    private TextView mLegalDataField;
    private String mHeaderData;
    private String mLegalData;


    private Handler mHandler;//handle BLE Scan Jobs


    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.title_devices);
        mHandler = new android.os.Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            this.mHeaderDataField.setText("Sorry, Your hardware does not support BLE.");
            //Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            //finish();//exit the app
        }


        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            this.mHeaderDataField.setText("Bluetooth service is not enabled.");
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            //finish();
            return;
        }


    }
}