package com.gianlu.aria2app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.Locale;
import java.util.Random;

public class DonutProgress extends View {
    private Paint arcPaint;
    private Paint transparentPaint;
    private Paint textPaint;
    private Rect textBound = new Rect();
    private String percentage;
    private float padding;
    private int sweepAngle;

    public DonutProgress(Context context) {
        super(context);
        init();
    }

    public DonutProgress(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        arcPaint = new Paint();
        arcPaint.setColor(Color.WHITE);
        arcPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Light.ttf"));
        textPaint.setAntiAlias(true);

        transparentPaint = new Paint();
        transparentPaint.setColor(Color.TRANSPARENT);
        transparentPaint.setAlpha(0xFF);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        if (isInEditMode())
            setProgress(new Random().nextInt(100));
        else
            setProgress(0);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    public void setFinishedStrokeColor(int color) {
        arcPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawArc(padding, padding, canvas.getWidth() - padding, canvas.getHeight() - padding, 270, sweepAngle, true, arcPaint);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, ((float) getWidth()) / 2 - padding * 2, transparentPaint);

        canvas.drawText(percentage, (getWidth() - textBound.width()) / 2, (getHeight() + textBound.height()) / 2, textPaint);
    }

    public void setProgress(float progress) {
        sweepAngle = (int) ((progress / 100) * 360);
        percentage = String.format(Locale.getDefault(), "%.1f", progress);
        textPaint.getTextBounds(percentage, 0, percentage.length(), textBound);

        invalidate();
    }
}
