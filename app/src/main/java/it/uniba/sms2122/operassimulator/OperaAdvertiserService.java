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
import android.os.Parcel;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OperaAdvertiserService extends IntentService {

    private static final String TAG = "OperaAdvertiserService";

    /**
     * Un uuid è formato da 128 bit. Tuttavia usarli tutti sarebbe dispendioso. Quindi nel caso del bluetooth, si usa un uuid a 16 bit..
     * L'uuid a 16 bit è fatto in questo modo: si un uuid completo a 128 fatto in questo modo: {@value} e si cambiano solo
     * i primi 8 caratteri. In questo caso i caratteri usati sono tutti uguali a 0. Utilizzare un UUID diverso, porta ad errori come "Data too large"
     * <br>
     * <a href="https://www.oreilly.com/library/view/getting-started-with/9781491900550/ch04.html">Fonte</a>
     */
    private static final String BASE_UUID = "00000000-0000-1000-8000-00805F9B34FB";

    private BluetoothLeAdvertiser advertiser;

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
        String dataHex = museumId + major + minor;
        byte[] serviceData = new byte[20];
        dataHex = dataHex.toUpperCase();
        for(int i=0; i<serviceData.length; i++) {
            serviceData[i] = (byte) (Integer.parseInt(dataHex.substring(i*2, i*2+2), 16) & 0xFF);
        }

        ParcelUuid parcelUuid = new ParcelUuid(UUID.fromString(BASE_UUID));

        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .build();
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceData(parcelUuid, serviceData)
                .addServiceUuid(parcelUuid)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "startAdvertising: permission error");
            return;
        }
        advertiser.startAdvertising(advertiseSettings, data, advertiseCallback);
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