package it.uniba.sms2122.operassimulator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import it.uniba.sms2122.operassimulator.utility.Permission;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView statusTextView;
    private Permission permission;
    private OperaAdvertiserService service;
    private boolean bounded = false;

    // Gestione del risultato dell'attivazione del bluetooth
    private final ActivityResultLauncher<Intent> btActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == Activity.RESULT_OK) {
                    Log.d("Bluetooth", "Acceso");
                    startService();
                } else {
                    Log.d("Bluetooth", "Non acceso");
                    enableBt();
                }
            }
    );

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            service = ((OperaAdvertiserService.LocalBinder) iBinder).getService();
            bounded = true;
            statusTextView.setText(R.string.started);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bounded = false;
            statusTextView.setText(R.string.stopped);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.status_text_view);
        permission = new Permission(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        /*
         * Se la versione è inferiore a quella indicata, il valore sarà vero, per cui non verranno controllati i permessi.
         * Se la versione è superiore, quindi il suo valore sarà gestito dai permessi.
         */
        boolean permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S;
        
        /*
         * Se non c'è il bluetooth, l'app si chiude mostrando un errore
         */
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, getString(R.string.bt_missing), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        /*
         * Procedo con il controllare i permessi
         */
        String[] permissions;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[] {Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH};
        } else {
            permissions = new String[] {Manifest.permission.BLUETOOTH};
        }
        permissionGranted = permission.getPermission(permissions, Permission.BLUETOOTH_PERMISSION_CODE,
                getString(R.string.bt_permission_title), getString(R.string.bt_permission_body));

        /*
         * Sulla base del valore booleano, faccio partire il bluetooth.
         */
        if(permissionGranted) {
            enableBt();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Permission.BLUETOOTH_PERMISSION_CODE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableBt();
                }

            default:
                Log.d(TAG, "onRequestPermissionsResult: default");
        }
    }

    private void enableBt() {
        BluetoothAdapter bluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        if(!bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            btActivityResultLauncher.launch(enableBluetooth);
        } else {
            startService();
        }
    }

    private void startService() {
        Intent intent = new Intent(this, OperaAdvertiserService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopService() {
        if(bounded) {
            unbindService(serviceConnection);
        }
    }

}