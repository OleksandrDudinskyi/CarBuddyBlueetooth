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
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Oleksandr Dudinskyi(dudinskyj@gmail.com)
 */
public class StartActivity extends ActionBarActivity {

    private static final String TAG = StartActivity.class.getCanonicalName();

    private static final int GET_CAR_BEACON = 1;
    private static final int GET_WALLET_BEACON = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private Button mPairCar;
    private Button mPairWallet;
    private Button mMonitorBtn;
    private TextView mCarBeaconName;
    private TextView mWalletBeaconName;
    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_CAR_BEACON:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    Message msg = Message.obtain(null, Constants.MSG_GET_CAR_ADDRESS, 0, 0);
                    String name = data.getExtras()
                            .getString(Constants.EXTRA_DEVICE_NAME);
                    mCarBeaconName.setText(name);
                    connectDevice(data, false, msg);
                }
                break;
            case GET_WALLET_BEACON:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    Message msg = Message.obtain(null, Constants.MSG_GET_WALLET_ADDRESS, 0, 0);
                    String name = data.getExtras()
                            .getString(Constants.EXTRA_DEVICE_NAME);
                    mWalletBeaconName.setText(name);
                    connectDevice(data, false, msg);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a session
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(StartActivity.this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link Constants#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure, Message msg) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(Constants.EXTRA_DEVICE_ADDRESS);
        // Create and send a message to the service, using a supported 'what' value
        Bundle bundle = new Bundle();
        bundle.putString(Constants.BEACON_ADDRESS, address);
        msg.setData(bundle);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        bindService(new Intent(StartActivity.this, MonitorService.class), mConnection,
                Context.BIND_AUTO_CREATE);
        mPairCar = (Button) findViewById(R.id.pair_car);
        mPairCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(StartActivity.this, BeaconsListActivity.class);
                startActivityForResult(serverIntent, GET_CAR_BEACON);
            }
        });
        mPairWallet = (Button) findViewById(R.id.pair_wallet);
        mPairWallet.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(StartActivity.this, BeaconsListActivity.class);
                startActivityForResult(serverIntent, GET_WALLET_BEACON);
            }
        });
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mCarBeaconName = (TextView) findViewById(R.id.car_beacon_name);
        mWalletBeaconName = (TextView) findViewById(R.id.wallet_beacon_name);

        mMonitorBtn = (Button) findViewById(R.id.monitor_btn);
        mMonitorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = Message.obtain(null, Constants.MSG_START_MONITOR, 0, 0);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Messenger for communicating with the service.
     */
    Messenger mService = null;

    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mBound;

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
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };
}
