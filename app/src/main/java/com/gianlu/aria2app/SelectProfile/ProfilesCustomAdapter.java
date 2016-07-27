package com.gianlu.aria2app.SelectProfile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gianlu.aria2app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfilesCustomAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<ProfileItem> profiles;
    private OnItemSelected onItemSelected;
    private OnItemEdit onItemEdit;
    private ExpandableListView listView;

    public ProfilesCustomAdapter(Context context, ExpandableListView listView, List<ProfileItem> profiles, OnItemSelected onItemSelected, OnItemEdit onItemEdit) {
        this.context = context;
        this.profiles = profiles;
        this.onItemSelected = onItemSelected;
        this.onItemEdit = onItemEdit;
        this.listView = listView;
    }

    @Override
    public int getGroupCount() {
        return profiles.size();
    }

    @Override
    public int getChildrenCount(int i) {
        if (getGroup(i).isSingleMode()) {
            return 0;
        } else {
            return ((MultiModeProfileItem) profiles.get(i)).getProfiles().size();
        }
    }

    @Override
    public ProfileItem getGroup(int i) {
        return profiles.get(i);
    }

    @Override
    public Pair<ConnectivityCondition, SingleModeProfileItem> getChild(int i, int i1) {
        ConnectivityCondition cond = new ArrayList<>(((MultiModeProfileItem) getGroup(i)).getProfiles().keySet()).get(i1);
        return new Pair<>(cond, ((MultiModeProfileItem) getGroup(i)).getProfiles().get(cond));
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        ProfileItem item = getGroup(i);
        return createGroupView(((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.profile_custom_single_item, null), i, item);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        Pair<ConnectivityCondition, SingleModeProfileItem> item = getChild(i, i1);
        return createChildView(((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.profile_custom_child, null), getGroup(i).getGlobalProfileName(), item.first, item.second);
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    private View createGroupView(final View view, final int position, final ProfileItem item) {
        TextView profileName = (TextView) view.findViewById(R.id.profileCustomItem_profileName);
        TextView serverIP = (TextView) view.findViewById(R.id.profileCustomItem_serverIP);
        TextView latency = (TextView) view.findViewById(R.id.profileCustomItem_latency);
        ImageView serverStatus = (ImageView) view.findViewById(R.id.profileCustomItem_serverStatus);
        ProgressBar serverProgressBar = (ProgressBar) view.findViewById(R.id.profileCustomItem_serverProgressBar);
        ImageButton select = (ImageButton) view.findViewById(R.id.profileCustomItem_select);
        ImageButton edit = (ImageButton) view.findViewById(R.id.profileCustomItem_edit);
        final ImageButton expand = (ImageButton) view.findViewById(R.id.profileCustomItem_expand);

        profileName.setText(item.getGlobalProfileName());
        latency.setText(String.format(Locale.getDefault(), "%s ms", item.getLatency() == -1 ? "-" : String.valueOf(item.getLatency())));
        serverIP.setText(item.isSingleMode() ? ((SingleModeProfileItem) item).getFullServerAddr() : ((MultiModeProfileItem) item).getCurrentProfile(context).getFullServerAddr());
        // TODO: Got default IP displayed here :/ (Multi)

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemSelected.onSelected(item.getGlobalProfileName(), item);
            }
        });
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemEdit.onEdit(item);
            }
        });

        if (!item.isSingleMode()) {
            expand.setVisibility(View.VISIBLE);
            expand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listView.isGroupExpanded(position)) {
                        listView.collapseGroup(position);
                        expand.setImageResource(R.drawable.ic_keyboard_arrow_right_black_48dp);
                    } else {
                        listView.expandGroup(position);
                        expand.setImageResource(R.drawable.ic_keyboard_arrow_down_black_48dp);
                    }

                    expand.invalidate();
                }
            });
        } else {
            expand.setVisibility(View.INVISIBLE);
        }

        switch (item.getStatus()) {
            case ONLINE:
                serverStatus.setVisibility(View.VISIBLE);
                serverProgressBar.setVisibility(View.INVISIBLE);
                serverStatus.setImageResource(R.drawable.ic_done_black_48dp);
                break;
            case OFFLINE:
                serverStatus.setVisibility(View.VISIBLE);
                serverProgressBar.setVisibility(View.INVISIBLE);
                serverStatus.setImageResource(R.drawable.ic_clear_black_48dp);
                break;
            case ERROR:
                serverStatus.setVisibility(View.VISIBLE);
                serverProgressBar.setVisibility(View.INVISIBLE);
                serverStatus.setImageResource(R.drawable.ic_error_black_48dp);
                break;
            case UNKNOWN:
                serverStatus.setVisibility(View.INVISIBLE);
                serverProgressBar.setVisibility(View.VISIBLE);
                serverProgressBar.setIndeterminate(true);
                break;
        }

        return view;
    }

    private View createChildView(View view, final String pprofileName, ConnectivityCondition condition, final SingleModeProfileItem profile) {
        ImageView profileType = (ImageView) view.findViewById(R.id.profileCustomChild_type);
        TextView profileName = (TextView) view.findViewById(R.id.profileCustomChild_profileName);
        TextView serverIP = (TextView) view.findViewById(R.id.profileCustomChild_serverIP);
        TextView latency = (TextView) view.findViewById(R.id.profileCustomChild_latency);
        ImageView serverStatus = (ImageView) view.findViewById(R.id.profileCustomChild_serverStatus);
        ProgressBar serverProgressBar = (ProgressBar) view.findViewById(R.id.profileCustomChild_serverProgressBar);
        ImageButton select = (ImageButton) view.findViewById(R.id.profileCustomChild_select);

        profileName.setText(profile.getProfileName());
        latency.setText(String.format(Locale.getDefault(), "%s ms", profile.getLatency() == -1 ? "-" : String.valueOf(profile.getLatency())));
        serverIP.setText(profile.getFullServerAddr());

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemSelected.onSelected(pprofileName, profile);
            }
        });

        switch (condition.getType()) {
            case WIFI:
                profileType.setImageResource(R.drawable.ic_network_wifi_black_48dp);
                break;
            case MOBILE:
                profileType.setImageResource(R.drawable.ic_network_cell_black_48dp);
                break;
            case ETHERNET:
                profileType.setImageResource(R.drawable.ic_settings_ethernet_black_48dp);
                break;
            case BLUETOOTH:
                profileType.setImageResource(R.drawable.ic_bluetooth_black_48dp);
                break;
            case UNKNOWN:
                profileType.setImageResource(R.drawable.ic_help_black_48dp);
                break;
        }


        switch (profile.getStatus()) {
            case ONLINE:
                serverStatus.setVisibility(View.VISIBLE);
                serverProgressBar.setVisibility(View.INVISIBLE);
                serverStatus.setImageResource(R.drawable.ic_done_black_48dp);
                break;
            case OFFLINE:
                serverStatus.setVisibility(View.VISIBLE);
                serverProgressBar.setVisibility(View.INVISIBLE);
                serverStatus.setImageResource(R.drawable.ic_clear_black_48dp);
                break;
            case ERROR:
                serverStatus.setVisibility(View.VISIBLE);
                serverProgressBar.setVisibility(View.INVISIBLE);
                serverStatus.setImageResource(R.drawable.ic_error_black_48dp);
                break;
            case UNKNOWN:
                serverStatus.setVisibility(View.INVISIBLE);
                serverProgressBar.setVisibility(View.VISIBLE);
                serverProgressBar.setIndeterminate(true);
                break;
        }

        return view;
    }

    public interface OnItemSelected {
        void onSelected(String profileName, ProfileItem item);
    }

    public interface OnItemEdit {
        void onEdit(ProfileItem item);
    }
}