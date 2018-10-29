package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SpinnerConditionsAdapter extends ArrayAdapter<MultiProfile.ConnectivityCondition> {
    private final Context context;

    public SpinnerConditionsAdapter(@NonNull Context context, @NonNull List<MultiProfile.ConnectivityCondition> objects) {
        super(context, android.R.layout.simple_spinner_dropdown_item, objects);
        this.context = context;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView text = (TextView) super.getDropDownView(position, convertView, parent);
        text.setTextColor(Color.WHITE);
        text.setText(getItem(position).getFormal(context));
        return text;
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView text = (TextView) super.getView(position, convertView, parent);
        text.setTextColor(Color.WHITE);
        text.setText(getItem(position).getFormal(context));
        return text;
    }
}
