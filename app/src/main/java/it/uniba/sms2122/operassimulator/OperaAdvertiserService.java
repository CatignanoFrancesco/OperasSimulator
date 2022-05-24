package it.uniba.sms2122.operassimulator;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

    private Map<String, Integer> activeServices;    // Gli id dei servizi attivi.
    private Map<String, OperaAdvertiser> activeAdvertisers; // Gli advertiser attivi.

    @Override
    public void onCreate() {
        super.onCreate();

        activeServices = new HashMap<>();
        activeAdvertisers = new HashMap<>();
    }

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

    /**
     * Crea un advertiser e fa partire l'advertising.
     * @param operaId L'id dell'opera di cui fare l'advertising
     * @param serviceUuid Il service uuid dell'adveriser
     * @param startId Lo start id del service
     */
    private void startAdvertising(String operaId, String serviceUuid, int startId) {
        if(!activeAdvertisers.containsKey(operaId)) {
            OperaAdvertiser operaAdvertiser = new OperaAdvertiser(this, operaId, serviceUuid);
            activeAdvertisers.put(operaId, operaAdvertiser);
            activeServices.put(operaId, startId);

            operaAdvertiser.startAdvertising();
        }
    }

    /**
     * Stoppa l'advertising di una determinata opera.
     * @param operaId L'id dell'opera di cui si vuole stoppare l'advertising
     */
    private void stopAdverting(String operaId) {
        if(activeAdvertisers.containsKey(operaId)) {
            OperaAdvertiser operaAdvertiser = activeAdvertisers.get(operaId);
            operaAdvertiser.stopAdvertising();
            activeAdvertisers.remove(operaId);
            int startId = activeServices.remove(operaId);
            stopSelf(startId);
        }
    }

    /**
     * Stoppa tutti gli advertising attivi.
     */
    private void stopAllAdvertising() {
        if(activeAdvertisers.size() > 0) {
            for(Map.Entry<String, OperaAdvertiser> entry : activeAdvertisers.entrySet()) {
                OperaAdvertiser operaAdvertiser = activeAdvertisers.get(entry.getKey());
                operaAdvertiser.stopAdvertising();
                int startId = activeServices.get(entry.getKey());
                stopSelf(startId);
            }
            activeAdvertisers.clear();
            activeServices.clear();
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
    /**
     * Metodo pubblico per far partire l'advertising di una determinata opera.
     * @param context Il contesto
     * @param operaId L'id dell'opera di cui si vuole fare l'advertising.
     * @param serviceUuid Il service uuid dell'advertiser.
     */
    public static void startService(Context context, String operaId, String serviceUuid) {
        Intent i = new Intent(context, OperaAdvertiserService.class);
        i.setAction(ACTION_START);
        i.putExtra("operaId", operaId);
        i.putExtra("serviceUuid", serviceUuid);
        context.startService(i);
    }

    /**
     * Metodo pubblico per stoppare uno specifico advertising.
     * @param context Il contesto
     * @param operaId L'id dell'opera di cui si vuole stoppare l'advertising.
     */
    public static void stopService(Context context, String operaId) {
        Intent i = new Intent(context, OperaAdvertiserService.class);
        i.setAction(ACTION_STOP);
        i.putExtra("operaId", operaId);
        context.startService(i);
    }

    /**
     * Metodo pubblico per stoppare tutti gli advertising attivi.
     * @param context Il contesto.
     */
    public static void stopAllServices(Context context) {
        Intent i = new Intent(context, OperaAdvertiserService.class);
        i.setAction(ACTION_STOP_ALL);
        context.startService(i);
    }

}