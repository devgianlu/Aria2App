package com.gianlu.aria2app;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;

import java.util.Objects;

public class FileTypeTextView extends AppCompatTextView {
    private int mWidth;

    public FileTypeTextView(Context context) {
        this(context, null, 0);
    }

    public FileTypeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FileTypeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWidth(32);
        setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Black.ttf"));

        if (isInEditMode()) setExtension("XML");
    }

    public void setWidth(int dip) {
        mWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
        setTextSize(dip);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
    }

    public void setFilename(@NonNull String filename) {
        String[] split = filename.split("\\.");
        if (split.length > 0) setExtension(split[split.length - 1]);
        else setExtension(null);
    }

    public void setExtension(@Nullable String ext) {
        if (Objects.equals(getText(), ext)) return;
        if (ext == null || ext.length() > 4) ext = "...";
        setText(ext.toUpperCase());
        adjustTextSize();
    }

    private void adjustTextSize() {
        TextPaint paint = getPaint();
        String text = String.valueOf(getText());

        while (paint.measureText(text) > mWidth)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize() - 1);
    }
}
