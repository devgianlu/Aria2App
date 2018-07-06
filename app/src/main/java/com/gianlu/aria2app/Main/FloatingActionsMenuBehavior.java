package com.gianlu.aria2app.Main;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.getbase.floatingactionbutton.AddFloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

@Keep
public class FloatingActionsMenuBehavior extends CoordinatorLayout.Behavior<FloatingActionsMenu> {
    private static final int DURATION = 150;
    private int mTotalDy = 0;

    public FloatingActionsMenuBehavior() {
    }

    public FloatingActionsMenuBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static boolean isScale(@NonNull FloatingActionsMenu view, float value) {
        View animateView = findInnerMenu(view);
        if (animateView == null) animateView = view;
        return animateView.getScaleX() == value;
    }

    static void scaleTo(@NonNull FloatingActionsMenu view, float value) {
        View animateView = findInnerMenu(view);
        if (animateView == null) animateView = view;
        animateView.animate().scaleY(value).scaleX(value).setDuration(DURATION).start();
    }

    @Nullable
    private static FloatingActionButton findInnerMenu(FloatingActionsMenu menu) {
        for (int i = 0; i < menu.getChildCount(); i++) {
            View v = menu.getChildAt(i);
            if (v instanceof AddFloatingActionButton)
                return (AddFloatingActionButton) v;
        }

        return null;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionsMenu child, @NonNull View directTargetChild, @NonNull View target, int nestedScrollAxes, @ViewCompat.NestedScrollType int type) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionsMenu child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @ViewCompat.NestedScrollType int type) {
        mTotalDy = dyConsumed < 0 && mTotalDy > 0 || dyConsumed > 0 && mTotalDy < 0 ? 0 : mTotalDy;
        if (mTotalDy == 0) ViewCompat.animate(child).cancel();
        mTotalDy += dyConsumed;

        int totalHeight = child.getHeight();
        if (mTotalDy > totalHeight && isScale(child, 1f)) {
            scaleTo(child, 0f);
        } else if (mTotalDy < 0 && Math.abs(mTotalDy) >= totalHeight && isScale(child, 0f)) {
            scaleTo(child, 1f);
        }
    }

    @Override
    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull final FloatingActionsMenu child, @NonNull View target, float velocityX, float velocityY) {
        if (Math.abs(velocityY) < Math.abs(velocityX)) return false;

        if (velocityY < 0) {
            /* Velocity is negative, we are flinging up */
            scaleTo(child, 1f);
        } else if (velocityY > 0) {
            /* Velocity is positive, we are flinging down */
            if (child.isExpanded()) {
                child.collapse();
                child.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
                    @Override
                    public void onMenuExpanded() {
                    }

                    @Override
                    public void onMenuCollapsed() {
                        if (isScale(child, 1f))
                            scaleTo(child, 0f);
                    }
                });
            } else {
                if (isScale(child, 1f)) scaleTo(child, 0f);
            }
        }

        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }
}
