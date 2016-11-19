package com.gianlu.aria2app.Profile;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;
import com.gianlu.commonutils.CommonUtils;

import java.util.Map;
import java.util.Set;

class ConditionsAdapter extends BaseAdapter {
    private static int currDefault = 0;
    private final Map<ConnectivityCondition, SingleModeProfileItem> objs;
    private final Activity context;
    private final OnClickListener edit;

    ConditionsAdapter(Activity context, Map<ConnectivityCondition, SingleModeProfileItem> objs, OnClickListener edit) {
        this.context = context;
        this.objs = objs;
        this.edit = edit;
    }

    @Override
    public int getCount() {
        return objs.size();
    }

    @Override
    public ConnectivityCondition getItem(int i) {
        Set<ConnectivityCondition> connectivityConditions = objs.keySet();
        return connectivityConditions.toArray(new ConnectivityCondition[connectivityConditions.size()])[i];
    }

    private SingleModeProfileItem getProfileItem(ConnectivityCondition condition) {
        return objs.get(condition);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(context).inflate(R.layout.condition_item, viewGroup, false));

        holder.condition.setText(getItem(i).getFormalName());
        holder.url.setText(getProfileItem(getItem(i)).getFullServerAddress());
        holder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edit.onClick(getProfileItem(getItem(i)), getItem(i));
            }
        });

        holder.isDefault.setChecked(i == currDefault);
        getProfileItem(getItem(i)).setDefault(i == currDefault);

        holder.isDefault.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    currDefault = i;
                else
                    CommonUtils.UIToast(context, Utils.ToastMessages.MUST_PICK_DEFAULT);
                notifyDataSetChanged();
            }
        });

        return holder.rootView;
    }

    public interface OnClickListener {
        void onClick(SingleModeProfileItem item, ConnectivityCondition condition);
    }

    public class ViewHolder {
        public final LinearLayout rootView;
        public final TextView condition;
        public final TextView url;
        public final ImageButton edit;
        public final CheckBox isDefault;

        ViewHolder(View rootView) {
            this.rootView = (LinearLayout) rootView;
            condition = (TextView) rootView.findViewById(R.id.conditionItem_condition);
            url = (TextView) rootView.findViewById(R.id.conditionItem_url);
            edit = (ImageButton) rootView.findViewById(R.id.conditionItem_edit);
            isDefault = (CheckBox) rootView.findViewById(R.id.conditionItem_default);
        }
    }
}
