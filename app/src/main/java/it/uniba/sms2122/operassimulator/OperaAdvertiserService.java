package it.uniba.sms2122.operassimulator;

import android.Manifest;
import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OperaAdvertiserService extends IntentService {

    private static final String TAG = "OperaAdvertiserService";
    private static final String MUSEUM_ID = "bbee7b2f-3f46-4920-9f54-043b4d979a74";

    private BluetoothLeAdvertiser advertiser;
    private AdvertisingSet currentAdvertisingSet;
    private int currentTxPower;

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            String error;
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    error = "Advertise already started";
                    break;

                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    error = "Too many advertisers";
                    break;

                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    error = "Data too large";
                    break;

                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    error = "Feature unsupported";
                    break;

                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    error = "Internal error";
                    break;

                default:
                    error = "Unknown error";
                    break;
            }
            Log.e(TAG, "onStartFailure: error: " + error);
        }
    };

    AdvertisingSetCallback advertisingSetCallback = new AdvertisingSetCallback() {
        @Override
        public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
            super.onAdvertisingSetStarted(advertisingSet, txPower, status);
            currentAdvertisingSet = advertisingSet;
            currentTxPower = txPower;
            changeParameters();
            Log.i(TAG, "onAdvertisingSetStarted: txPower: " + txPower + ", status: " + status);
        }

        @Override
        public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
            super.onAdvertisingSetStopped(advertisingSet);
            Log.i(TAG, "onAdvertisingSetStopped():");
        }

        @Override
        public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
            super.onAdvertisingDataSet(advertisingSet, status);
            Log.i(TAG, "onAdvertisingDataSet(): status:" + status);
        }

        @Override
        public void onScanResponseDataSet(AdvertisingSet advertisingSet, int status) {
            super.onScanResponseDataSet(advertisingSet, status);
            Log.i(TAG, "onScanResponseDataSet(): status:" + status);
        }
    };

    private final IBinder binder = new LocalBinder();

    public OperaAdvertiserService() {
        super("OperaAdvertiserService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        startAdvertising();
        return binder;
    }

    private void startAdvertising() {
        advertiser = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getBluetoothLeAdvertiser();

        String museumId = "bbee7b2f3f4649209f54043b4d979a74";
        String major = "0000";
        String minor = "0001";
        String dataHex = "1bffffffbeac" + museumId + major + minor + "bb" + "0000000000000000000000000000000000000000000000000000000000000000000000";   // 70
        byte[] serviceData = new byte[62];
        dataHex = dataHex.toUpperCase();
        for(int i=0; i<serviceData.length; i++) {
            serviceData[i] = (byte) (Integer.parseInt(dataHex.substring(i*2, i*2+2), 16) & 0xFF);
        }

        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .build();
        AdvertisingSetParameters parameters = new AdvertisingSetParameters.Builder()
                .setLegacyMode(true)
                .setConnectable(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .build();
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceData(new ParcelUuid(UUID.fromString(MUSEUM_ID.toUpperCase())), serviceData)
                .addServiceUuid(new ParcelUuid(UUID.fromString(MUSEUM_ID.toUpperCase())))
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "startAdvertising: permission error");
            return;
        }
        advertiser.startAdvertising(advertiseSettings, data, advertiseCallback);
        //advertiser.startAdvertisingSet(parameters, data, null, null, null, advertisingSetCallback);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopAdvertising();
        return super.onUnbind(intent);
    }

    public void stopAdvertising() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "stopAdvertising: permission error");
            return;
        }
        advertiser.stopAdvertising(advertiseCallback);
        //advertiser.stopAdvertisingSet(advertisingSetCallback);
    }

    private void changeParameters() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "changeParameters: permission error");
            return;
        }

        currentAdvertisingSet.setAdvertisingData(new AdvertiseData.Builder()
                .setIncludeDeviceName(true).setIncludeTxPowerLevel(true).build());
        currentAdvertisingSet.setScanResponseData(new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(UUID.fromString(MUSEUM_ID))).build());
    }

    private byte[] getAdvertisingData(String operaId) {
        int advertisingDataDimension = 62;
        String startingInfo = "1bffffffbeac";
        byte[] advertisingData = new byte[advertisingDataDimension];
        Charset charset = StandardCharsets.UTF_16LE;

        return advertisingData;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {}


    /**
     * Classe che estende il binder
     */
    public class LocalBinder extends Binder {
        OperaAdvertiserService getService() {
            return OperaAdvertiserService.this;
        }
    }
}