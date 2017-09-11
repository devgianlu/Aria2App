package com.gianlu.aria2app.Main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

@SuppressLint("unused")
public class FloatingActionsMenuBehavior extends CoordinatorLayout.Behavior<FloatingActionsMenu> {
    public FloatingActionsMenuBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionsMenu child, View dependency) {
        return dependency instanceof RecyclerView;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionsMenu child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return true;
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionsMenu child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        if (child.getTag() == null) child.setTag(true);
        if (dyConsumed > 0 && ((boolean) child.getTag())) {
            child.setTag(false);
            child.animate().translationY(child.getHeight()).setDuration(20).start();
        } else if (dyConsumed < 0 && (!(boolean) child.getTag())) {
            child.setTag(true);
            child.animate().translationY(0).setDuration(150).start();
        }
    }
}
