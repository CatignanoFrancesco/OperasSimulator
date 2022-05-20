package it.uniba.sms2122.operassimulator;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

import it.uniba.sms2122.operassimulator.model.Opera;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.OperaViewHolder> {
    private MainActivity mainActivity;
    private ArrayList<Opera> opere;
    private HashMap<String, String> serviceUuids = new HashMap<>();

    public RecyclerViewAdapter(MainActivity mainActivity, ArrayList<Opera> opere) {
        this.opere = opere;
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public OperaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mainActivity);
        View view = inflater.inflate(R.layout.opera_list_row, parent, false);
        return new OperaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OperaViewHolder holder, int position) {
        String operaId = opere.get(position).getId();
        holder.btSwitch.setText(operaId);
        serviceUuids.put(operaId, operaId.substring(operaId.length()-4));
        
        holder.btSwitch.setOnClickListener(view -> {
            if(holder.btSwitch.isChecked()) {
                mainActivity.startService(serviceUuids.get(operaId), operaId);
                Toast.makeText(mainActivity, mainActivity.getString(R.string.bt_started, operaId), Toast.LENGTH_SHORT).show();
            } else {
                mainActivity.stopService(operaId);
                Toast.makeText(mainActivity, mainActivity.getString(R.string.bt_stopped, operaId), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return opere.size();
    }


    static class OperaViewHolder extends RecyclerView.ViewHolder {
        SwitchCompat btSwitch;

        public OperaViewHolder(@NonNull View itemView) {
            super(itemView);
            btSwitch = itemView.findViewById(R.id.bluetooth_advertisement_switch);
        }
    }
}
