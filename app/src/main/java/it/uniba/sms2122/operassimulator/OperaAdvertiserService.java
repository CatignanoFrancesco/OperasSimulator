package it.uniba.sms2122.operassimulator;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    private static final String FIRST_BASE_UUID = "0000";
    private static final String PREFIX = "it.uniba.sms2122.operassimulator.service.";

    public static final String ACTION_START = PREFIX + "ACTION_START";
    public static final String ACTION_STOP = PREFIX + "ACTION_STOP";
    public static final String ACTION_STOP_ALL = PREFIX + "ACTION_STOP_ALL";

    private String serviceUuid;
    private String operaId;

    private final Map<String, Integer> advertisements = new HashMap<>();

    private BluetoothLeAdvertiser advertiser;
    private AdvertisingSetCallback advertisingSetCallback;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        serviceUuid = intent.hasExtra("serviceUuid") ? "" : intent.getStringExtra("serviceUuid");
        operaId = intent.getStringExtra("operaId");

        switch(action) {
            case ACTION_START:
                startAdvertising(FIRST_BASE_UUID+serviceUuid+LAST_BASE_UUID, operaId, startId);
                break;
            case ACTION_STOP:
                stopAdvertising(operaId);
                break;
            default:
                Log.d(TAG, "onStartCommand: default");
                break;
        }

        return START_NOT_STICKY;
    }

    /**
     * Metodo per cominciare l'advertising
     * @param serviceUuid Il service uuid del del beacon
     * @param dataHex I dati da trasmettere
     */
    private void startAdvertising(String serviceUuid, String dataHex, int startId) {

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
        advertiser.startAdvertisingSet(parameters, data, null, null, null, new OperaAdvertisingSetCallback(operaId));
        advertisements.put(operaId, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: service destroyed");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: service unbounded");
        return super.onUnbind(intent);
    }

    /**
     * Stoppa la trasmissione dei dati tramite bluetooth.
     */
    private void stopAdvertising(String operaId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "stopAdvertising: permission error");
            return;
        }

        if(advertisements.containsKey(operaId) && advertisements.get(operaId) != null) {
            advertiser.stopAdvertisingSet(new OperaAdvertisingSetCallback(operaId));
        }
    }



    public class OperaAdvertisingSetCallback extends AdvertisingSetCallback {
        private static final String TAG = "OperaAdvertisingSetCallback";

        private String operaId;

        public OperaAdvertisingSetCallback(String operaId) {
            this.operaId = operaId;
        }

        @Override
        public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
            super.onAdvertisingSetStarted(advertisingSet, txPower, status);
            logAdvertisingSetStatus("onAdvertisingSetStarted", status);
        }

        @Override
        public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
            super.onAdvertisingSetStopped(advertisingSet);
            Log.i(TAG, "onAdvertisingSetStopped, " + operaId);
            stopSelf(advertisements.remove(operaId));
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

        /**
         * Effettua il log dei messaggi ottenuti in AdvertisingSetCallback
         * @param functionName
         * @param status
         */
        private void logAdvertisingSetStatus(String functionName, int status) {
            switch (status) {
                case AdvertisingSetCallback.ADVERTISE_SUCCESS:
                    Log.i(TAG, functionName + ": Advertise success, " + operaId);
                    break;

                case AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Log.e(TAG, functionName + ": Data too large, " + operaId);
                    break;

                case AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Log.e(TAG, functionName + ": Too many advertisers, " + operaId);
                    break;

                case AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.e(TAG, functionName + ": Already started, " + operaId);
                    break;

                case AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    Log.e(TAG, functionName + ": Internal error, " + operaId);
                    break;

                case AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, functionName + ": Feature unsupported, " + operaId);
                    break;

                default:
                    Log.e(TAG, functionName + ": Unknown error, " + operaId);
                    break;
            }
        }
    }


}