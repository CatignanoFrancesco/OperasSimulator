package it.uniba.sms2122.operassimulator;

import android.Manifest;
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
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import it.uniba.sms2122.operassimulator.model.Opera;

public class OperaAdvertiserService extends Service {

    private static final String TAG = "OperaAdvertiserService";


    private static final String PREFIX = "it.uniba.sms2122.operassimulator.service.";

    public static final String ACTION_START = PREFIX + "ACTION_START";
    public static final String ACTION_STOP = PREFIX + "ACTION_STOP";
    public static final String ACTION_STOP_ALL = PREFIX + "ACTION_STOP_ALL";

    private String serviceUuid;
    private String operaId;

    private final Map<String, Integer> advertisements = new HashMap<>();
    private final Map<String, OperaAdvertiser> advertisers = new HashMap<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        String serviceUuid = !intent.hasExtra("serviceUuid") ? "" : intent.getStringExtra("serviceUuid");
        String operaId = !intent.hasExtra("operaId") ? "" : intent.getStringExtra("operaId");

        switch(action) {
            case ACTION_START:
                startAdvertising(operaId, serviceUuid, startId);
                break;
            case ACTION_STOP:
                stopAdverting(operaId);
                break;
            case ACTION_STOP_ALL:
                stopAllAdvertising();
            default:
                Log.d(TAG, "onStartCommand: default");
                break;
        }

        return START_NOT_STICKY;
    }

    private void startAdvertising(String operaId, String serviceUuid, int startId) {
        if(!advertisers.containsKey(operaId)) {
            OperaAdvertiser operaAdvertiser = new OperaAdvertiser(this, operaId, serviceUuid);
            advertisers.put(operaId, operaAdvertiser);
            advertisements.put(operaId, startId);

            operaAdvertiser.startAdvertising();
        }
    }

    private void stopAdverting(String operaId) {
        if(advertisers.containsKey(operaId)) {
            OperaAdvertiser operaAdvertiser = advertisers.get(operaId);
            operaAdvertiser.stopAdvertising();
            advertisers.remove(operaId);
            int startId = advertisements.remove(operaId);
            stopSelf(startId);
        }
    }

    private void stopAllAdvertising() {
        if(advertisers.size() > 0) {
            for(Map.Entry<String, OperaAdvertiser> entry : advertisers.entrySet()) {
                OperaAdvertiser operaAdvertiser = advertisers.get(entry.getKey());
                operaAdvertiser.stopAdvertising();
                int startId = advertisements.get(entry.getKey());
                stopSelf(startId);
            }
            advertisers.clear();
            advertisements.clear();
        }
    }

    public static void startService(Context context, String operaId, String serviceUuid) {
        Intent i = new Intent(context, OperaAdvertiserService.class);
        i.setAction(ACTION_START);
        i.putExtra("operaId", operaId);
        i.putExtra("serviceUuid", serviceUuid);
        context.startService(i);
    }

    public static void stopService(Context context, String operaId) {
        Intent i = new Intent(context, OperaAdvertiserService.class);
        i.setAction(ACTION_STOP);
        i.putExtra("operaId", operaId);
        context.startService(i);
    }

    public static void stopAllServices(Context context) {
        Intent i = new Intent(context, OperaAdvertiserService.class);
        i.setAction(ACTION_STOP_ALL);
        context.startService(i);
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