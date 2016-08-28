package com.gianlu.aria2app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class LetterIconSmall extends View {
    private Context context;
    private String letters;
    private Rect lettersBounds = new Rect();
    private Paint shapePaint;
    private Paint letterPaint;

    public LetterIconSmall(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public LetterIconSmall setProfileName(String name) {
        if (name == null) {
            setVisibility(GONE);
            return this;
        }

        if (name.length() <= 2) letters = name;
        letters = name.substring(0, 2);

        return this;
    }

    public LetterIconSmall setTextColor(@ColorRes int colorRes) {
        letterPaint = new Paint();
        letterPaint.setColor(ContextCompat.getColor(context, colorRes));
        letterPaint.setAntiAlias(true);
        letterPaint.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf"));
        letterPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, context.getResources().getDisplayMetrics()));

        return this;
    }

    public LetterIconSmall setShapeColor(@ColorRes int colorRes, @ColorRes int shadowColor) {
        shapePaint = new Paint();
        shapePaint.setAntiAlias(true);
        shapePaint.setColor(ContextCompat.getColor(context, colorRes));
        shapePaint.setShadowLayer(4, 0, 4, ContextCompat.getColor(context, shadowColor));
        setLayerType(LAYER_TYPE_SOFTWARE, shapePaint);

        return this;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int viewWidthHalf = this.getMeasuredWidth() / 2;
        int viewHeightHalf = this.getMeasuredHeight() / 2;

        int radius;
        if (viewWidthHalf > viewHeightHalf)
            radius = viewHeightHalf - 4;
        else
            radius = viewWidthHalf - 4;

        canvas.drawCircle(viewWidthHalf, viewHeightHalf, radius, shapePaint);

        letterPaint.getTextBounds(letters, 0, letters.length(), lettersBounds);

        canvas.drawText(letters, viewWidthHalf - lettersBounds.exactCenterX(), viewHeightHalf - lettersBounds.exactCenterY(), letterPaint);
    }

    public void build() {
        invalidate();
    }
}
