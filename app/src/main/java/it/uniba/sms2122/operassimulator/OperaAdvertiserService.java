package it.uniba.sms2122.operassimulator;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class OperaAdvertiserService extends Service {
    private static final String TAG = "OperaAdvertiserService";
    private static final String PREFIX = "it.uniba.sms2122.operassimulator.service.";
    public static final String ACTION_START = PREFIX + "ACTION_START";
    public static final String ACTION_STOP = PREFIX + "ACTION_STOP";
    public static final String ACTION_STOP_ALL = PREFIX + "ACTION_STOP_ALL";

    private static Map<String, OperaAdvertiser> activeAdvertisers; // Gli advertiser attivi.
    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        activeAdvertisers = new HashMap<>();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        String serviceUuid = !intent.hasExtra("serviceUuid") ? "" : intent.getStringExtra("serviceUuid");
        String operaId = !intent.hasExtra("operaId") ? "" : intent.getStringExtra("operaId");

        switch(action) {
            case ACTION_START:
                startAdvertising(operaId, serviceUuid);
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

    /**
     * Crea un advertiser e fa partire l'advertising.
     * @param operaId L'id dell'opera di cui fare l'advertising
     * @param serviceUuid Il service uuid dell'adveriser
     */
    public void startAdvertising(String operaId, String serviceUuid) {
        if(!activeAdvertisers.containsKey(operaId)) {
            OperaAdvertiser operaAdvertiser = new OperaAdvertiser(this, operaId, serviceUuid);
            activeAdvertisers.put(operaId, operaAdvertiser);

            operaAdvertiser.startAdvertising();
        }
    }

    /**
     * Stoppa l'advertising di una determinata opera.
     * @param operaId L'id dell'opera di cui si vuole stoppare l'advertising
     */
    public void stopAdverting(String operaId) {
        if(activeAdvertisers.containsKey(operaId)) {
            OperaAdvertiser operaAdvertiser = activeAdvertisers.get(operaId);
            operaAdvertiser.stopAdvertising();
            activeAdvertisers.remove(operaId);
        }
    }

    /**
     * Stoppa tutti gli advertising attivi.
     */
    public void stopAllAdvertising() {
        if(activeAdvertisers.size() > 0) {
            for(Map.Entry<String, OperaAdvertiser> entry : activeAdvertisers.entrySet()) {
                OperaAdvertiser operaAdvertiser = activeAdvertisers.get(entry.getKey());
                operaAdvertiser.stopAdvertising();
            }
            activeAdvertisers.clear();
        }
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


    /*
     ******************************************
     *             METODI PUBBLICI
     * ****************************************
     *
     */


    public class LocalBinder extends Binder {
        public OperaAdvertiserService getService() {
            return OperaAdvertiserService.this;
        }
    }

}