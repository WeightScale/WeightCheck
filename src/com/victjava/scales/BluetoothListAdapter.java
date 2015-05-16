package com.victjava.scales;

/**
 * Created by IntelliJ IDEA.
 * User: Kostya
 * Date: 23.10.11
 * Time: 9:20
 * To change this template use File | Settings | File Templates.
 */

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

class BluetoothListAdapter extends ArrayAdapter<BluetoothDevice> {
    private final ArrayList<BluetoothDevice> items;

    public BluetoothListAdapter(Context context, ArrayList<BluetoothDevice> items) {
        super(context, R.layout.list_item_bluetooth, items);
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.list_item_bluetooth, parent, false);
        }

        if (v == null) {
            return null;
        }
        BluetoothDevice o = items.get(position);
        if (o != null) {
            TextView tt = (TextView) v.findViewById(R.id.topText);
            TextView bt = (TextView) v.findViewById(R.id.bottomText);
            //LinearLayout linearLayout = (LinearLayout)v.findViewById(R.id.layout_item_bluetooth);
            /*if(position == 0)
                linearLayout.setBackgroundResource(R.color.background_light);*/
            if (tt != null) {
                tt.setText(o.getName());
            }
            if (bt != null) {
                String address = o.getAddress();
                if (tt != null) {
                    if ("-".equalsIgnoreCase(address)) {
                        tt.setTextColor(0xFFFF5050);
                    } else
                    //tt.setTextColor(getContext().getResources().getColor(R.color.tt_item_bt));
                    {
                        tt.setTextColor(getContext().getResources().getColor(R.color.white));
                    }
                }
                bt.setText(address);
            }
        }
        return v;
    }

    @Override
    public void add(BluetoothDevice object) {
        super.add(object);
    }
}
