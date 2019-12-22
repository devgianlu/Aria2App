package com.gianlu.aria2app.main;

import android.animation.Animator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

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

    private static boolean isScaleMax(@NonNull FloatingActionsMenu view) {
        View animateView = findInnerMenu(view);
        if (animateView == null) animateView = view;
        return animateView.getScaleX() == 1;
    }

    static void scaleTo(@NonNull final FloatingActionsMenu view, final boolean max) {
        View animateView = findInnerMenu(view);
        if (animateView == null) animateView = view;
        animateView.animate().scaleY(max ? 1 : 0).scaleX(max ? 1 : 0).setDuration(DURATION).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (max) view.setTranslationX(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!max) view.setTranslationX(view.getWidth());
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).start();
    }

    @Nullable
    private static FloatingActionButton findInnerMenu(@NonNull FloatingActionsMenu menu) {
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
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionsMenu child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
        mTotalDy = dyConsumed < 0 && mTotalDy > 0 || dyConsumed > 0 && mTotalDy < 0 ? 0 : mTotalDy;
        if (mTotalDy == 0) ViewCompat.animate(child).cancel();
        mTotalDy += dyConsumed;

        int totalHeight = child.getHeight();
        if (mTotalDy > totalHeight && isScaleMax(child)) {
            scaleTo(child, false);
        } else if (mTotalDy < 0 && Math.abs(mTotalDy) >= totalHeight && !isScaleMax(child)) {
            scaleTo(child, true);
        }
    }

    @Override
    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull final FloatingActionsMenu child, @NonNull View target, float velocityX, float velocityY) {
        if (Math.abs(velocityY) < Math.abs(velocityX)) return false;

        if (velocityY < 0) {
            /* Velocity is negative, we are flinging up */
            scaleTo(child, true);
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
                        if (isScaleMax(child))
                            scaleTo(child, false);
                    }
                });
            } else {
                if (isScaleMax(child)) scaleTo(child, false);
            }
        }

        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }
}
