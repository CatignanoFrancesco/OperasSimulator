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

public class OperaAdvertiserService extends Service {

    private static final String TAG = "OperaAdvertiserService";

    /**
     * Un uuid è formato da 128 bit. Tuttavia usarli tutti sarebbe dispendioso. Quindi nel caso del bluetooth, si usa un uuid a 16 bit..
     * L'uuid a 16 bit è fatto in questo modo: si un uuid completo a 128 fatto in questo modo: xxxxxxxx{@value} e si cambiano solo
     * i primi 8 caratteri. In questo caso i caratteri usati sono tutti uguali a 0. Utilizzare un UUID diverso, porta ad errori come "Data too large"
     * <br>
     * <a href="https://www.oreilly.com/library/view/getting-started-with/9781491900550/ch04.html">Fonte</a>
     */
    private static final String LAST_BASE_UUID = "-0000-1000-8000-00805F9B34FB";

    private String serviceUuid;
    private String operaId;

    private BluetoothLeAdvertiser advertiser;
    private AdvertisingSetCallback advertisingSetCallback;
    private final IBinder binder = new LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceUuid = intent.getStringExtra("serviceUuid");
        operaId = intent.getStringExtra("operaId");
        startAdvertising(serviceUuid+LAST_BASE_UUID, operaId);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void startAdvertising(String serviceUuid, String dataHex) {
        AdvertisingSetCallback advertisingSetCallback = new AdvertisingSetCallback() {
            @Override
            public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                super.onAdvertisingSetStarted(advertisingSet, txPower, status);
                logAdvertisingSetStatus("onAdvertisingSetStarted", status);
            }

            @Override
            public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
                super.onAdvertisingSetStopped(advertisingSet);
                Log.i(TAG, "onAdvertisingSetStopped");
            }

            @Override
            public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
                super.onAdvertisingDataSet(advertisingSet, status);
                logAdvertisingSetStatus("onAdvertisingDataSet", status);
            }

            @Override
            public void onScanResponseDataSet(AdvertisingSet advertisingSet, int status) {
                super.onScanResponseDataSet(advertisingSet, status);
                logAdvertisingSetStatus("onScanResponseDataSet", status);
            }
        };

        this.advertisingSetCallback = advertisingSetCallback;

        advertiser = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getBluetoothLeAdvertiser();

        byte[] serviceData = new byte[20];
        dataHex = dataHex.toUpperCase();
        for(int i=0; i<serviceData.length; i++) {
            serviceData[i] = (byte) (Integer.parseInt(dataHex.substring(i*2, i*2+2), 16) & 0xFF);
        }

        ParcelUuid parcelUuid = new ParcelUuid(UUID.fromString(serviceUuid));

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "startAdvertising: permission error");
            return;
        }
        advertiser.startAdvertisingSet(parameters, data, null, null, null, advertisingSetCallback);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopAdvertising();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAdvertising();
    }

    public void stopAdvertising() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "stopAdvertising: permission error");
            return;
        }
        advertiser.stopAdvertisingSet(advertisingSetCallback);
    }

    /**
     * Effettua il log dei messaggi ottenuti in AdvertisingSetCallback
     * @param functionName
     * @param status
     */
    private void logAdvertisingSetStatus(String functionName, int status) {
        switch (status) {
            case AdvertisingSetCallback.ADVERTISE_SUCCESS:
                Log.i(TAG, functionName + ": Advertise success");
                break;

            case AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                Log.e(TAG, functionName + ": Data too large");
                break;

            case AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                Log.e(TAG, functionName + ": Too many advertisers");
                break;

            case AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                Log.e(TAG, functionName + ": Already started");
                break;

            case AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                Log.e(TAG, functionName + ": Internal error");
                break;

            case AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                Log.e(TAG, functionName + ": Feature unsupported");
                break;

            default:
                Log.e(TAG, functionName + ": Unknown error");
                break;
        }
    }


    /**
     * Classe che estende il binder
     */
    public class LocalBinder extends Binder {
        OperaAdvertiserService getService() {
            return OperaAdvertiserService.this;
        }
    }
}