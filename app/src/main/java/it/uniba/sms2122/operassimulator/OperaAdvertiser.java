package it.uniba.sms2122.operassimulator;

import android.Manifest;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.UUID;

public class OperaAdvertiser {
    private static final String TAG = "OperaAdvertiser";
    /**
     * Un uuid è formato da 128 bit. Tuttavia usarli tutti sarebbe dispendioso. Quindi nel caso del bluetooth, si usa un uuid a 16 bit..
     * L'uuid a 16 bit è fatto in questo modo: si un uuid completo a 128 fatto in questo modo: xxxxxxxx{@value} e si cambiano solo
     * i primi 8 caratteri. In questo caso i caratteri usati sono tutti uguali a 0. Utilizzare un UUID diverso, porta ad errori come "Data too large"
     * <br>
     * <a href="https://www.oreilly.com/library/view/getting-started-with/9781491900550/ch04.html">Fonte</a>
     */
    private static final String LAST_BASE_UUID = "-0000-1000-8000-00805F9B34FB";
    private static final String FIRST_BASE_UUID = "0000";

    private final String operaId;
    private final String serviceUuid;
    private boolean advertising = false;
    private final Context context;
    private BluetoothLeAdvertiser advertiser;
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "onStartSuccess: started "+ operaId);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "onStartFailure: Already started, " + operaId);
                    break;

                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Log.e(TAG, "onStartFailure: Too many advertisers, " + operaId);
                    break;

                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Log.e(TAG, "onStartFailure: Data too large, " + operaId);
                    break;

                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    Log.e(TAG, "onStartFailure: Internal error, " + operaId);
                    break;

                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "onStartFailure: Feature unsupported, " + operaId);
                    break;

                default:
                    Log.e(TAG, "onStartFailure: default, " + operaId);
            }
        }
    };

    public OperaAdvertiser(Context context, String operaId, String serviceUuid) {
        this.context = context;
        this.operaId = operaId;
        this.serviceUuid = FIRST_BASE_UUID + serviceUuid + LAST_BASE_UUID;
        advertiser = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getBluetoothLeAdvertiser();
    }

    public void startAdvertising() {
        byte[] serviceData = new byte[20];
        for(int i=0; i<serviceData.length; i++) {
            serviceData[i] = (byte) (Integer.parseInt(operaId.toUpperCase().substring(i*2, i*2+2), 16) & 0xFF);
        }

        ParcelUuid parcelUuid = new ParcelUuid(UUID.fromString(serviceUuid));

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(false)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
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
                .addServiceData(parcelUuid, serviceData)
                .addServiceUuid(parcelUuid)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "startAdvertising: permission error");
            return;
        }
        advertiser.startAdvertising(settings, data, advertiseCallback);
        advertising = true;
    }

    public void stopAdvertising() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "stopAdvertising: permission error");
            return;
        }

        if(advertiser != null) {
            advertiser.stopAdvertising(advertiseCallback);
            advertising = false;
            advertiser = null;
            Log.d(TAG, "stopAdvertising: " + operaId);
        }
    }

    public boolean isAdvertising() {
        return advertising;
    }
}
