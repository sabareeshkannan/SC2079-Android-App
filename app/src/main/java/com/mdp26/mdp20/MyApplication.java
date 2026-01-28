package com.mdp26.mdp20;

import android.app.Application;

import com.mdp26.mdp20.bluetooth.BluetoothConnection;
import com.mdp26.mdp20.bluetooth.BluetoothInterface;
import com.mdp26.mdp20.canvas.Grid;
import com.mdp26.mdp20.canvas.Robot;

/**
 * Application class to hold app-scoped variables, such as the bluetooth connection handler.
 */
public class MyApplication extends Application {
    private BluetoothInterface bluetoothInterface;
    private Grid grid;
    private Robot robot;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Apply saved theme preference globally at app startup
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DarkMode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES 
                       : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );
        
        bluetoothInterface = new BluetoothInterface(this);
        grid = new Grid();
        robot = Robot.ofDefault();
    }

    //getters omit "get" to showcase immutability, liken to records
    public BluetoothInterface btInterface() {
        return bluetoothInterface;
    }

    public BluetoothConnection btConnection() {
        return bluetoothInterface.getBluetoothConnection();
    }

    public Grid grid() {
        return grid;
    }

    public Robot robot() {
        return robot;
    }
}