package it.uniba.sms2122.operassimulator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

import it.uniba.sms2122.operassimulator.model.Opera;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.OperaViewHolder> {
    private final MainActivity mainActivity;
    private static ArrayList<Opera> opere;
    private static HashMap<String, String> serviceUuids;

    public RecyclerViewAdapter(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        opere = new ArrayList<>();
        serviceUuids = new HashMap<>();
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
        holder.btSwitch.setChecked(false);
        serviceUuids.put(operaId, operaId.substring(operaId.length()-4));

        holder.btSwitch.setOnCheckedChangeListener((compoundButton, bChecked) -> {
            if(bChecked) {
                mainActivity.startAdvertising(operaId, serviceUuids.get(operaId));
                Toast.makeText(mainActivity, mainActivity.getString(R.string.bt_started, operaId), Toast.LENGTH_SHORT).show();
            } else {
                mainActivity.stopAdvertising(operaId);
                Toast.makeText(mainActivity, mainActivity.getString(R.string.bt_stopped, operaId), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Pulisce la recycler view
     */
    public void clear() {
        opere.clear();
        serviceUuids.clear();
    }

    /**
     * Aggiunge le opere alla recycler view
     * @param opere Le opere da aggiungere
     */
    public void addOperas(ArrayList<Opera> opere) {
        RecyclerViewAdapter.opere.addAll(opere);
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
            btSwitch.setChecked(false);
        }
    }
}
