/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.polkapolka.bluetooth.le.ui;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.*;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.polkapolka.bluetooth.le.R;
import com.polkapolka.bluetooth.le.attr.SampleGattAttributes;
import com.polkapolka.bluetooth.le.service.BluetoothLeService;
import com.polkapolka.bluetooth.le.util.MathHelper;
import com.polkapolka.bluetooth.le.util.ViewHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends SherlockActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView isSerial;
    private TextView mConnectionState;
    private TextView mDataField;

    private int mScrollVal = 0;
    private SeekBar mScroll;//for analog turning....


    private double sensorRawTempVal=0.0;

    private String mDeviceName;
    private String mDeviceAddress;
    //  private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;


    public final static UUID HM_RX_TX =
            UUID.fromString(SampleGattAttributes.HM_RX_TX);

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    RoundKnobButton mRotateBtn;

    @InjectView(R.id.rotate_btn_parent)
    RelativeLayout mRotateBtnParent;

    @InjectView(R.id.hidden_black_small_circle)
    ImageView mHiddenBlackSmallCircle;

    float[] mSmallCircleSize = {0, 0};

    final float[] DEGREE_RANGE = {45, 135 + 180};

    float mCirclesRadius;

    final int POINT_NUM = 24;

    ArrayList<ImageView> mSmallBlackCircleArray = new ArrayList<ImageView>();

    ArrayList<ImageView> mSmallWhiteCircleArray = new ArrayList<ImageView>();

    private boolean mHasPartInit = false;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        ButterKnife.inject(this);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        // is serial present?
        isSerial = (TextView) findViewById(R.id.isSerial);

        mDataField = (TextView) findViewById(R.id.data_value);
        mScroll = (SeekBar) findViewById(R.id.mScroll);

        readSeek(mScroll);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mRotateBtn = new RoundKnobButton(this,
                R.drawable.rotate_btn,
                R.drawable.rotate_btn,
                (int) ViewHelper.convertDpToPixel(this, getResources().getDimension(R.dimen.rotate_btn)),
                (int) ViewHelper.convertDpToPixel(this, getResources().getDimension(R.dimen.rotate_btn)),
                DEGREE_RANGE);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mRotateBtnParent.addView(mRotateBtn, layoutParams);

        mRotateBtn.setRotorPercentage(0);

        mRotateBtn.setListener(new RoundKnobButton.RoundKnobButtonListener() {
            @Override
            public void onStateChange(boolean newstate) {}

            @Override
            public void onRotate(int percentage) {
                Log.d(TAG, "rotate percentage: " + percentage);
                controlSmallCircles(percentage);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (mHasPartInit == false) {
            doPartInit();
        }
    }

    private void doPartInit() {
        mHasPartInit = true;

        mSmallCircleSize = new float[]{mHiddenBlackSmallCircle.getWidth(), mHiddenBlackSmallCircle.getHeight()};
        Log.d(TAG, "small circle size: w: " + mSmallCircleSize[0] + ", h: " + mSmallCircleSize[1]);
        mCirclesRadius = mRotateBtn.getWidth() / 2 + 20;

        generateCircle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.menu_connect == item.getItemId()) {
            mBluetoothLeService.connect(mDeviceAddress);
            return true;
        } else if (R.id.menu_disconnect == R.id.menu_connect) {
            mBluetoothLeService.disconnect();
            return true;
        } else if (android.R.id.home == R.id.menu_connect) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {

        if (data != null) {
            mDataField.setText(data);
        }
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));

            // If the service exists for HM 10 Serial, say so.
            if (SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
                isSerial.setText("Yes, serial :-)");
            } else {
                isSerial.setText("No, serial :-(");
            }
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
        }

    }

    //App intent
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    //seekBar event handle goes here...
    private void readSeek(SeekBar seekBar) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mScrollVal = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                makeChange();
            }
        });
    }

    // on change of bars write char
    private void makeChange() {
        String str = String.format("SeekBar Val:%s\n",mScrollVal);
        Log.i(TAG, "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if (mConnected) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }
    }

    private void controlSmallCircles(int percentage) {
        int num = mSmallWhiteCircleArray.size() * percentage / 100;
        if (percentage == 99) {
            num += 1;
        }
        Log.d(TAG, "white light num: " + num + ", white lights total num: " + mSmallWhiteCircleArray.size());
        for (int i = 0; i < num; ++i) {
            mSmallWhiteCircleArray.get(i).setVisibility(View.VISIBLE);
        }
        for (int i = num; i < mSmallWhiteCircleArray.size(); ++i) {
            mSmallWhiteCircleArray.get(i).setVisibility(View.GONE);
        }
    }

    private void generateCircle() {
        int centerX = mRotateBtnParent.getWidth();
        int centerY = mRotateBtnParent.getHeight();
        Log.d(TAG, "target parent view: width: " + centerX + ", pvY: " + centerY);
        ArrayList<ArrayList<Float>> pointsPositionArray = MathHelper.generateCirclePositionArray(centerX / 2, centerY / 2, DEGREE_RANGE[0], DEGREE_RANGE[1], mCirclesRadius, POINT_NUM);
        int num = pointsPositionArray.size();
        for (int i = 0; i < num; ++i) {
            ArrayList<Float> position = pointsPositionArray.get(i);
            ImageView v = new ImageView(this);
            v.setBackgroundResource(R.drawable.black_small_circle);
            mRotateBtnParent.addView(v, new FrameLayout.LayoutParams((int) getResources().getDimension(R.dimen.small_circl_radius), (int) getResources().getDimension(R.dimen.small_circl_radius)));
            float[] leftTopPosition = ViewHelper.convertCenterPosition2LeftTopPosition(mSmallCircleSize[0], mSmallCircleSize[1], position.get(0), position.get(1));
            v.setX(leftTopPosition[0]);
            v.setY(leftTopPosition[1]);
            mSmallBlackCircleArray.add(v);

            ImageView v2 = new ImageView(this);
            v2.setBackgroundResource(R.drawable.white_small_circle);
            mRotateBtnParent.addView(v2, new FrameLayout.LayoutParams((int) getResources().getDimension(R.dimen.small_circl_radius), (int) getResources().getDimension(R.dimen.small_circl_radius)));
            leftTopPosition = ViewHelper.convertCenterPosition2LeftTopPosition(mSmallCircleSize[0], mSmallCircleSize[1], position.get(0), position.get(1));
            v2.setX(leftTopPosition[0]);
            v2.setY(leftTopPosition[1]);
            v2.setVisibility(View.GONE);
            mSmallWhiteCircleArray.add(v2);
        }
    }


}