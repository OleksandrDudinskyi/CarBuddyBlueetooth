package com.dudinskyi.carbuddyapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Oleksandr Dudinskyi(dudinskyj@gmail.com)
 */
public class StartActivity extends ActionBarActivity implements View.OnClickListener {

    private static final String TAG = StartActivity.class.getCanonicalName();

    private static final int GET_CAR_BEACON = 1;
    private static final int GET_WALLET_BEACON = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int GET_SETTINGS = 4;
    private Button mSetupCar;
    private Button mSetupWallet;
    private CheckBox mMonitorBtn;
    private TextView mCarBeaconName;
    private TextView mWalletBeaconName;
    private String mCarBeaconAddress;
    private String mWalletBeaconAddress;
    private int mRSSISettings = 60;
    private int mUpdateTimeSettings = 10;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        bindService(new Intent(StartActivity.this, MonitorService.class), mConnection,
                Context.BIND_AUTO_CREATE);
        mSetupCar = (Button) findViewById(R.id.setup_car);
        mCarBeaconName = (TextView) findViewById(R.id.car_beacon_name);
        mWalletBeaconName = (TextView) findViewById(R.id.wallet_beacon_name);
        mMonitorBtn = (CheckBox) findViewById(R.id.monitor_btn);
        mSetupWallet = (Button) findViewById(R.id.setup_wallet);
        mSetupCar.setOnClickListener(this);
        mSetupWallet.setOnClickListener(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final String monitorStartTitle = getResources().getString(R.string.monitor_btn_start_title);
        final String monitorStopTitle = getResources().getString(R.string.monitor_btn_stop_title);
        mMonitorBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Message msg;
                if (isChecked) {
                    mMonitorBtn.setText(monitorStartTitle);
                    bindService(new Intent(StartActivity.this, MonitorService.class), mConnection,
                            Context.BIND_AUTO_CREATE);
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.CAR_BEACON_ADDRESS, mCarBeaconAddress);
                    bundle.putString(Constants.WALLET_BEACON_ADDRESS, mWalletBeaconAddress);
                    bundle.putInt(Constants.EXTRA_SETTINGS_RSSI, mRSSISettings);
                    bundle.putInt(Constants.EXTRA_SETTINGS_UPDATE_TIME, mUpdateTimeSettings);
                    msg = Message.obtain(null, Constants.MSG_START_MONITOR, 0, 0);
                    msg.setData(bundle);
                } else {
                    mMonitorBtn.setText(monitorStopTitle);
                    msg = Message.obtain(null, Constants.MSG_STOP_MONITOR, 0, 0);
                }
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(StartActivity.this, SettingsActivity.class);
            settingsIntent.putExtra(Constants.EXTRA_SETTINGS_RSSI, mRSSISettings);
            settingsIntent.putExtra(Constants.EXTRA_SETTINGS_UPDATE_TIME, mUpdateTimeSettings);
            startActivityForResult(settingsIntent, GET_SETTINGS);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_CAR_BEACON:
                // When @link BeaconsListActivity returns car beacon set up it
                if (resultCode == Activity.RESULT_OK) {
                    // Create and send a message to the service, using a supported 'what' value
                    Message msg = Message.obtain(null, Constants.MSG_GET_CAR_ADDRESS, 0, 0);
                    String name = data.getExtras().getString(Constants.EXTRA_DEVICE_NAME);
                    // Get the device MAC address
                    String address = data.getExtras().getString(Constants.EXTRA_DEVICE_ADDRESS);
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.BEACON_ADDRESS, address);
                    msg.setData(bundle);
                    mCarBeaconAddress = data.getExtras().getString(Constants.EXTRA_DEVICE_ADDRESS);
                    mCarBeaconName.setText(name);
                    setupBeacon(msg);
                }
                break;
            case GET_WALLET_BEACON:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Create and send a message to the service, using a supported 'what' value
                    Message msg = Message.obtain(null, Constants.MSG_GET_WALLET_ADDRESS, 0, 0);
                    String name = data.getExtras().getString(Constants.EXTRA_DEVICE_NAME);
                    // Get the device MAC address
                    String address = data.getExtras().getString(Constants.EXTRA_DEVICE_ADDRESS);
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.BEACON_ADDRESS, address);
                    msg.setData(bundle);
                    mWalletBeaconAddress = data.getExtras().getString(Constants.EXTRA_DEVICE_ADDRESS);
                    mWalletBeaconName.setText(name);
                    setupBeacon(msg);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode != Activity.RESULT_OK) {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(StartActivity.this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            case GET_SETTINGS:
                if (resultCode == Activity.RESULT_OK) {
                    mRSSISettings = data.getExtras().getInt(Constants.EXTRA_SETTINGS_RSSI, 0);
                    mUpdateTimeSettings = data.getExtras().getInt(Constants.EXTRA_SETTINGS_UPDATE_TIME, 0);
                    // Create and send a message to the service, using a supported 'what' value
                    Message msg = Message.obtain(null, Constants.MSG_SETTINGS, 0, 0);
                    Bundle bundle = new Bundle();
                    bundle.putInt(Constants.EXTRA_SETTINGS_RSSI, mRSSISettings);
                    bundle.putInt(Constants.EXTRA_SETTINGS_UPDATE_TIME, mUpdateTimeSettings);
                    msg.setData(bundle);
                    setupBeacon(msg);
                }
                break;
            default:
                break;
        }

    }

    /**
     * Setup beacon and send beacon address to service
     *
     * @param data An {@link Intent} with {@link Constants#EXTRA_DEVICE_ADDRESS} extra.
     * @param msg  Message to monitor service
     */
    private void setupBeacon(Message msg) {
        if (mService != null) {
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Messenger for communicating with the service.
     */
    Messenger mService = null;

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.setup_car:
                // Launch the BeaconListActivity to see devices and do scan
                intent = new Intent(StartActivity.this, BeaconsListActivity.class);
                startActivityForResult(intent, GET_CAR_BEACON);
                break;
            case R.id.setup_wallet:
                // Launch the BeaconListActivity to see devices and do scan
                intent = new Intent(StartActivity.this, BeaconsListActivity.class);
                startActivityForResult(intent, GET_WALLET_BEACON);
                break;
        }
    }
}
