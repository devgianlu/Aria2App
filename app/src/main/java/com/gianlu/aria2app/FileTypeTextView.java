package com.gianlu.aria2app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.gianlu.commonutils.FontsManager;

public class FileTypeTextView extends View {
    private final TextPaint textPaint;
    private final float mDefaultTextSize;
    private final int mMaxHeight;
    private String mText;
    private StaticLayout mLayout;

    public FileTypeTextView(Context context) {
        this(context, null, 0);
    }

    public FileTypeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FileTypeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);

        textPaint = new TextPaint();
        textPaint.setTypeface(FontsManager.get().get(context, FontsManager.ROBOTO_BLACK));
        textPaint.setAntiAlias(true);

        mDefaultTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FileTypeTextView, defStyleAttr, 0);
        try {
            mMaxHeight = a.getDimensionPixelSize(R.styleable.FileTypeTextView_maxHeight, (int) mDefaultTextSize);
            textPaint.setColor(a.getColor(R.styleable.FileTypeTextView_textColor, Color.BLACK));
        } finally {
            a.recycle();
        }

        if (isInEditMode()) setExtension("XML");
    }

    public static float getFitTextSize(@NonNull TextPaint paint, float width, @NonNull String text) {
        float nowWidth = paint.measureText(text);
        return width / nowWidth * paint.getTextSize();
    }

    public void setFilename(@NonNull String filename) {
        int pos = filename.lastIndexOf('.');
        if (pos != -1) setExtension(filename.substring(pos + 1, filename.length()));
        else setExtension(null);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mText != null) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            textPaint.setTextSize(mDefaultTextSize);
            textPaint.setTextSize(Math.min(getFitTextSize(textPaint, width, mText), mMaxHeight) - 1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mLayout = StaticLayout.Builder.obtain(mText, 0, mText.length(), textPaint, width)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .setIncludePad(false).build();
            } else {
                mLayout = new StaticLayout(mText, textPaint, width, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
            }

            int height = Math.min(mLayout.getHeight() - mLayout.getTopPadding() - mLayout.getBottomPadding(), mMaxHeight);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mLayout != null) mLayout.draw(canvas);
    }

    public void setExtension(@Nullable String ext) {
        if (ext == null) {
            ext = "?";
        } else {
            ext = ext.trim();
            if (ext.length() > 4) ext = ext.substring(0, 4);
        }

        mText = ext.toUpperCase();
        requestLayout();
        invalidate();
    }
}
