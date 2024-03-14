package com.halvaor.gamingknights.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.halvaor.gamingknights.R;

public class CustomAdapter extends BaseAdapter {

    String itemName;
    Context context;
    LayoutInflater inflater;


    public CustomAdapter(Context ctx, String itemName) {
        inflater = LayoutInflater.from(ctx);
        this.itemName = itemName;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = inflater.inflate(R.layout.view_item, null);
        TextView textView = (TextView) convertView.findViewById(R.id.view_item);
        textView.setText(itemName);

        return convertView;
    }
}
