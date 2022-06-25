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

    private static Map<String, OperaAdvertiser> activeAdvertisers; // Gli advertiser attivi.
    private final IBinder binder = new LocalBinder();
    private boolean bound = false;

    @Override
    public void onCreate() {
        super.onCreate();
        activeAdvertisers = new HashMap<>();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        bound = true;
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        stopAllAdvertising();
        bound = false;
        return super.onUnbind(intent);
    }

    public boolean isBound() {
        return bound;
    }

    public class LocalBinder extends Binder {
        public OperaAdvertiserService getService() {
            return OperaAdvertiserService.this;
        }
    }

}