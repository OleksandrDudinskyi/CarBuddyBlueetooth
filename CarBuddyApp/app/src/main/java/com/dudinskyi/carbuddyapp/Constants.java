package com.dudinskyi.carbuddyapp;

/**
 * Defines several constants
 *
 * @author Oleksandr Dudinskyi(dudinskyj@gmail.com)
 */
public interface Constants {
    public static final String BEACON_ADDRESS = "beacon_address";
    public static final String CAR_BEACON_ADDRESS = "car_beacon_address";
    public static final String WALLET_BEACON_ADDRESS = "wallet_beacon_address";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_SETTINGS_RSSI = "settings_rssi";
    public static final String EXTRA_SETTINGS_UPDATE_TIME = "update_time";
    public static final int MSG_GET_CAR_ADDRESS = 1;
    public static final int MSG_GET_WALLET_ADDRESS = 2;
    public static final int MSG_SETTINGS = 3;
    public static final int MSG_START_MONITOR = 4;
    public static final int MSG_STOP_MONITOR = 5;
}
