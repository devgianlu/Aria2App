package com.gianlu.aria2app.Main;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

@Keep
public class FloatingActionsMenuBehavior extends CoordinatorLayout.Behavior<FloatingActionsMenu> {
    private static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutLinearInInterpolator();
    private int mTotalDy = 0;

    public FloatingActionsMenuBehavior() {
    }

    public FloatingActionsMenuBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static boolean isScaleX(View v, float value) {
        return v.getScaleX() == value;
    }

    private static void scaleTo(View view, float value) {
        ViewPropertyAnimatorCompat viewPropertyAnimatorCompat = ViewCompat.animate(view)
                .scaleX(value).scaleY(value).setDuration(100)
                .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR);
        viewPropertyAnimatorCompat.start();
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionsMenu child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
            child.setTranslationY(translationY);
        }

        return false;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionsMenu child, @NonNull View directTargetChild, @NonNull View target, int nestedScrollAxes, @ViewCompat.NestedScrollType int type) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionsMenu child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @ViewCompat.NestedScrollType int type) {
        mTotalDy = dyConsumed < 0 && mTotalDy > 0 || dyConsumed > 0 && mTotalDy < 0 ? 0 : mTotalDy;
        attemptCancelAnimation(child);
        mTotalDy += dyConsumed;

        int totalHeight = child.getHeight();
        if (mTotalDy > totalHeight && isScaleX(child, 1f)) {
            scaleTo(child, 0f);
        } else if (mTotalDy < 0 && Math.abs(mTotalDy) >= totalHeight && isScaleX(child, 0f)) {
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
                        if (isScaleX(child, 1f))
                            scaleTo(child, 0f);
                    }
                });
            } else {
                if (isScaleX(child, 1f)) scaleTo(child, 0f);
            }
        }

        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }

    private void attemptCancelAnimation(FloatingActionsMenu child) {
        if (mTotalDy == 0) {
            ViewCompat.animate(child).cancel();
            ViewCompat.animate(child.getChildAt(child.getChildCount() - 1)).cancel();
        }
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent, FloatingActionsMenu child, View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout && child.getTranslationY() != 0.0F) {
            ViewCompat.animate(child).translationY(0.0F).scaleX(1.0F).scaleY(1.0F).alpha(1.0F)
                    .setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR).setListener(null);
        }
    }
}
