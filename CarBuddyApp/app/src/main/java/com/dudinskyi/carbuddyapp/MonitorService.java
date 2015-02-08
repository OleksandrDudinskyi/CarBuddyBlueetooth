package com.dudinskyi.carbuddyapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Monitor service for receiving car and wallet beacons addresses and monitoring their visibility
 *
 * @author Oleksandr Dudinskyi(dudinskyj@gmail.com)
 */
public class MonitorService extends Service {

    private BluetoothAdapter mBtAdapter;
    private String mCarBeaconAddress;
    private String mWalletBeaconAddress;
    private boolean isWalletFound;
    private boolean isCarFound;
    private ScheduledThreadPoolExecutor executor =
            new ScheduledThreadPoolExecutor(1);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private short mCarRSSI = 0;
    private short mWalletRSSI = 0;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBluetoothReceiver);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Register for broadcasts when a device is discovered
        IntentFilter foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBluetoothReceiver, foundFilter);

        // Register for broadcasts when discovery has finished
        IntentFilter discoveryFinishedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBluetoothReceiver, discoveryFinishedFilter);
    }

    /**
     * The BroadcastReceiver that listens for discovered devices
     */
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                // Check if found device one of our beacon
                if (device.getAddress().equals(mCarBeaconAddress)) {
                    mCarRSSI = rssi;
                    isCarFound = true;
                } else if (device.getAddress().equals(mWalletBeaconAddress)) {
                    mWalletRSSI = rssi;
                    isWalletFound = true;
                }
            }
        }

        ;
    };

    private void notifyUser() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.wallet_icon)
                        .setContentTitle("Car Buddy App")
                        .setContentText("It seems you forgot your pocket!!!");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(getApplicationContext(), StartActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(StartActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(1, mBuilder.build());
    }

    private Runnable timerTask = new Runnable() {

        @Override
        public void run() {
            // If we're already discovering, stop it
            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }
            Log.d("test","car rssi: " + mCarRSSI);
            Log.d("test","wallet rssi: " + mWalletRSSI);
            if (isCarFound && mCarRSSI > -70 && (mWalletRSSI < -70 || !isWalletFound)) {
                notifyUser();
            }
            isCarFound = false;
            isWalletFound = false;
            // Request discover from BluetoothAdapter
            mBtAdapter.startDiscovery();
        }

    };

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_GET_CAR_ADDRESS:
                    mCarBeaconAddress = msg.getData().getString(Constants.BEACON_ADDRESS);
                    break;
                case Constants.MSG_GET_WALLET_ADDRESS:
                    mWalletBeaconAddress = msg.getData().getString(Constants.BEACON_ADDRESS);
                    break;
                case Constants.MSG_START_MONITOR:
                    mCarBeaconAddress = msg.getData().getString(Constants.CAR_BEACON_ADDRESS);
                    mWalletBeaconAddress = msg.getData().getString(Constants.WALLET_BEACON_ADDRESS);
                    if (executor.isShutdown()) {
                        executor = new ScheduledThreadPoolExecutor(1);
                    }
                    executor.scheduleAtFixedRate(timerTask, 0, 15, TimeUnit.SECONDS);
                    break;
                case Constants.MSG_STOP_MONITOR:
                    executor.shutdown();
                    stopSelf();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
