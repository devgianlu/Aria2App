package com.gianlu.aria2app.Main;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gianlu.aria2app.R;

public class DrawerManager {
    private Activity context;
    private DrawerLayout drawerLayout;
    private LinearLayout drawerList;
    private LinearLayout drawerFooterList;
    private IDrawerListener listener;


    public DrawerManager(Activity context, DrawerLayout drawerLayout) {
        this.context = context;
        this.drawerLayout = drawerLayout;
        this.drawerList = (LinearLayout) drawerLayout.findViewById(R.id.mainDrawer_list);
        this.drawerFooterList = (LinearLayout) drawerLayout.findViewById(R.id.mainDrawer_footerList);
    }

    public void setDrawerListener(IDrawerListener listener) {
        this.listener = listener;
    }

    public void setDrawerState(boolean open) {
        if (open)
            drawerLayout.openDrawer(GravityCompat.START, true);
        else
            drawerLayout.closeDrawer(GravityCompat.START, true);
    }

    private View newItem(@DrawableRes int icon, String title, @Nullable String description, boolean primary) {
        return newItem(icon, title, description, primary, -1);
    }

    private View newItem(@DrawableRes int icon, String title, @Nullable String description, boolean primary, int badgeNumber) {
        int textColor;
        if (primary)
            textColor = Color.BLACK;
        else
            textColor = ContextCompat.getColor(context, R.color.colorPrimary_ripple);

        View view = View.inflate(context, R.layout.material_drawer_item_primary, null);
        ((ImageView) view.findViewById(R.id.material_drawer_icon)).setImageResource(icon);
        ((TextView) view.findViewById(R.id.material_drawer_name)).setText(title);
        ((TextView) view.findViewById(R.id.material_drawer_name)).setTextColor(textColor);
        if (description == null) {
            view.findViewById(R.id.material_drawer_description).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.material_drawer_description).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.material_drawer_description)).setText(description);
            ((TextView) view.findViewById(R.id.material_drawer_description)).setTextColor(textColor);
        }
        if (badgeNumber == -1) {
            view.findViewById(R.id.material_drawer_badge_container).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.material_drawer_badge_container).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.material_drawer_badge)).setText(String.valueOf(badgeNumber));
        }

        return view;
    }

    public void updateBadge(int num) {
        View view = drawerList.getChildAt(0);

        if (num == -1) {
            view.findViewById(R.id.material_drawer_badge_container).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.material_drawer_badge_container).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.material_drawer_badge)).setText(String.valueOf(num));
        }
    }

    public void build() {
        drawerList.removeAllViews();

        View home = newItem(R.mipmap.ic_launcher, context.getString(R.string.home), null, true, 0);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onListItemSelected(DrawerListItems.HOME);
            }
        });
        drawerList.addView(home, 0);

        View terminal = newItem(R.mipmap.ic_launcher, context.getString(R.string.terminal), null, true);
        terminal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onListItemSelected(DrawerListItems.TERMINAL);
            }
        });
        drawerList.addView(terminal);

        View globalOptions = newItem(R.mipmap.ic_launcher, context.getString(R.string.menu_globalOptions), null, true);
        globalOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onListItemSelected(DrawerListItems.GLOBAL_OPTIONS);
            }
        });
        drawerList.addView(globalOptions);

        // Footer group
        drawerFooterList.removeAllViews();

        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundResource(R.color.colorPrimary_ripple);
        drawerFooterList.addView(divider, 0);

        View preferences = newItem(R.mipmap.ic_launcher, context.getString(R.string.menu_preferences), null, false);
        preferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onListItemSelected(DrawerListItems.PREFERENCES);
            }
        });
        drawerFooterList.addView(preferences);
    }

    public enum DrawerListItems {
        HOME,
        TERMINAL,
        GLOBAL_OPTIONS,
        PREFERENCES
    }

    public interface IDrawerListener {
        void onListItemSelected(DrawerListItems which);
    }
}
