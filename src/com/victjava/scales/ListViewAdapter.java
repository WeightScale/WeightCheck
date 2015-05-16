package com.victjava.scales;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ListViewAdapter extends ArrayAdapter<Component> {

    public ListViewAdapter(Context context, int textViewResourceId, List<Component> objects) {
        super(context, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View curView = convertView;
        if (curView == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            curView = vi.inflate(R.layout.item_list_dialog, null);
        }

        Component cp = getItem(position);
        TextView title = (TextView) curView.findViewById(R.id.title);
        title.setText(cp.getTitle());
        return curView;
    }


}

class Component {

    private String title;
    private String subtitle;

    public Component(String t, String sub) {
        // TODO Auto-generated constructor stub
        title = t;
        subtitle = sub;

    }


    public CharSequence getTitle() {
        return title;
    }

    public void setTitle(String s) {

        title = s;
    }


    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String s) {
        subtitle = s;
    }


}
