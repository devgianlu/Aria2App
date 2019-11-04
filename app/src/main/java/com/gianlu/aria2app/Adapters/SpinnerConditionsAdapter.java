package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.ProfilesManager.MultiProfile;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import java.util.List;

public class SpinnerConditionsAdapter extends ArrayAdapter<MultiProfile.ConnectivityCondition> {
    private final Context context;

    public SpinnerConditionsAdapter(@NonNull Context context, @NonNull List<MultiProfile.ConnectivityCondition> objects) {
        super(context, android.R.layout.simple_spinner_dropdown_item, objects);
        this.context = context.getApplicationContext();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView text = (TextView) super.getDropDownView(position, convertView, parent);
        CommonUtils.setTextColorFromAttr(text, R.attr.colorOnSurface);
        text.setText(getItem(position).getFormal(context));
        return text;
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView text = (TextView) super.getView(position, convertView, parent);
        CommonUtils.setTextColor(text, R.color.white);
        text.setText(getItem(position).getFormal(context));
        return text;
    }
}
