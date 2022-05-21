package it.uniba.sms2122.operassimulator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.uniba.sms2122.operassimulator.model.Opera;
import it.uniba.sms2122.operassimulator.model.Stanza;
import it.uniba.sms2122.operassimulator.utility.Permission;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String JSON_MIME_TYPE = "application/json";

    private TextView roomNameTV;
    private Button addRoomButton;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private ConstraintLayout container;
    private Menu menu;

    private Permission permission;
    private Map<String, Intent> startedServices;
    private Stanza selectedStanza;

    // Gestione del risultato dell'attivazione del bluetooth
    private final ActivityResultLauncher<Intent> btActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == Activity.RESULT_OK) {
                    Log.d("Bluetooth", "Acceso");
                } else {
                    Log.d("Bluetooth", "Non acceso");
                    enableBt();
                }
            }
    );

    private final ActivityResultLauncher<Intent> jsonRoomActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == Activity.RESULT_OK) {
                    // Prendo l'uri
                    Uri uri;
                    if(result.getData() != null) {
                        uri = result.getData().getData();
                        String mimeType = getContentResolver().getType(uri);

                        if(mimeType.equals(JSON_MIME_TYPE)) {
                            // Apro il file
                            try (Reader reader = new InputStreamReader(getContentResolver().openInputStream(uri))) {
                                selectedStanza = new Gson().fromJson(reader, Stanza.class);
                                changeState();
                            }
                            catch(Exception ex) {
                                showGenericErrorDialog();
                                Log.e(TAG, ex.getMessage());
                            }
                        } else {
                            Log.e(TAG, "Errore non previsto sul file aperto");
                        }
                    } else {
                        showGenericErrorDialog();
                        Log.e(TAG, "uri is null");
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addRoomButton = findViewById(R.id.add_stanza_btn);
        container = findViewById(R.id.operas_container);
        recyclerView = findViewById(R.id.operas_list);
        recyclerViewAdapter = new RecyclerViewAdapter(this);
        permission = new Permission(this);
        startedServices = new HashMap<>();
        roomNameTV = findViewById(R.id.room_name);

        addRoomButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(JSON_MIME_TYPE);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            jsonRoomActivityLauncher.launch(intent);
        });

        recyclerView.setAdapter(recyclerViewAdapter);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.advertising, menu);
        this.menu = menu;
        menu.findItem(R.id.trash).setVisible(addRoomButton.getVisibility() == View.GONE);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.trash) {
            changeState();
            stopAllServices();
            return true;
        }
        return false;
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
        stopAllServices();
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
        }
    }

    public void startService(String serviceUuid, String operaId) {
        Intent intent = new Intent(this, OperaAdvertiserService.class);
        intent.putExtra("serviceUuid", serviceUuid);
        intent.putExtra("operaId", operaId);
        startService(intent);
        startedServices.put(operaId, intent);
    }

    public void stopService(String operaId) {
        if(startedServices.containsKey(operaId)) {
            stopService(startedServices.get(operaId));
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    private void stopAllServices() {
        Iterator<Map.Entry<String, Intent>> i = startedServices.entrySet().iterator();

        while(i.hasNext()) {
            stopService(i.next().getKey());
            i.remove();
        }
    }

    private void showGenericErrorDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.error)
            .setMessage(R.string.error_message)
            .setNeutralButton(R.string.ok, null)
            .show();
    }

    private void changeState() {
        boolean isListVisible = addRoomButton.getVisibility() == View.GONE;
        recyclerViewAdapter.clear();
        recyclerViewAdapter.notifyDataSetChanged();

        if(!isListVisible) {
            ArrayList<Opera> opere = new ArrayList<>();
            for(Map.Entry<String, Opera> entry : selectedStanza.getOpere().entrySet()) {
                opere.add(entry.getValue());
            }

            recyclerViewAdapter.addOperas(opere);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            roomNameTV.setText(selectedStanza.getNome());
            addRoomButton.setVisibility(View.GONE);
            container.setVisibility(View.VISIBLE);
            isListVisible = true;
        } else {
            container.setVisibility(View.GONE);
            addRoomButton.setVisibility(View.VISIBLE);
            isListVisible = false;
        }

        menu.findItem(R.id.trash).setVisible(isListVisible);
    }

}