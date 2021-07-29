package com.example.sendhelpapplication;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.Task;
import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.ConnectionPriority;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;


import java.util.ArrayList;
import java.util.UUID;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


import static com.example.sendhelpapplication.SendHelpApp.CHANNEL_ID;

public class SendHelpService extends Service {

    ArrayList<String> phoneNumbersList;

    private static final UUID sendHelpServiceUUID = UUID.fromString("00005321-0000-1000-8000-00805f9b34fb");
    private static final UUID sendHelpCharacteristicUUID = UUID.fromString("00000001-0000-1000-8000-00805f9b34fb");

    public BluetoothCentralManager central;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private FusedLocationProviderClient fusedLocationClient;

    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(@org.jetbrains.annotations.NotNull BluetoothPeripheral peripheral) {
            super.onServicesDiscovered(peripheral);

            BluetoothGattCharacteristic sendHelpCharacteristic = peripheral.getCharacteristic(sendHelpServiceUUID, sendHelpCharacteristicUUID);

            // Request a new connection priority
            peripheral.requestConnectionPriority(ConnectionPriority.HIGH);

            peripheral.setNotify(sendHelpCharacteristic, true);
        }

        @Override
        public void onCharacteristicUpdate(@org.jetbrains.annotations.NotNull BluetoothPeripheral peripheral, @org.jetbrains.annotations.NotNull byte[] value, @org.jetbrains.annotations.NotNull BluetoothGattCharacteristic characteristic, @org.jetbrains.annotations.NotNull GattStatus status) {
            super.onCharacteristicUpdate(peripheral, value, characteristic, status);

            if (status != GattStatus.SUCCESS) return;

            UUID characteristicUUID = characteristic.getUuid();
            BluetoothBytesParser parser = new BluetoothBytesParser(value);

            if (characteristicUUID.equals(sendHelpCharacteristicUUID)) {
                parser = new BluetoothBytesParser(value);

                int flags = parser.getIntValue(0x11);
                if (flags == 1) {
                    sendMessage();
                }
            }
        }
    };

    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDisconnectedPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            super.onDisconnectedPeripheral(peripheral, status);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    central.autoConnectPeripheral(peripheral, peripheralCallback);
                }
            }, 10000);
        }

        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            super.onDiscoveredPeripheral(peripheral, scanResult);

            central.stopScan();
            central.connectPeripheral(peripheral, peripheralCallback);
        }
    };

    private void startScan() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                central.scanForPeripheralsWithServices(new UUID[]{sendHelpServiceUUID});
            }
        }, 2000);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        phoneNumbersList = intent.getStringArrayListExtra("inputExtra");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Send Help Service")
                .setContentText("Selected Phone Numbers Will Be Alerted.")
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        central = new BluetoothCentralManager(getApplicationContext(), bluetoothCentralManagerCallback, handler);

        startScan();

        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void sendMessage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();

        SmsManager smgr = SmsManager.getDefault();
        locationTask.addOnSuccessListener(new  OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                double latitude, longitude;

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                String txtMessage = "Help! I have requested help at " + String.valueOf(latitude) + ", " + String.valueOf(longitude);
                String googleMapsAddress = "https://www.google.com/maps/search/?api=1&query=" + String.valueOf(latitude) + "%2C" + String.valueOf(longitude);

                for (String phoneNumber : phoneNumbersList) {
                    smgr.sendTextMessage(phoneNumber, null, txtMessage, null, null);
                    smgr.sendTextMessage(phoneNumber, null, googleMapsAddress, null, null);
                }
            }
        });
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
