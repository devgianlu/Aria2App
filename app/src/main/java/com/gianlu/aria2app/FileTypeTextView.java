package com.gianlu.aria2app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.gianlu.commonutils.FontsManager;

import java.util.Objects;

public class FileTypeTextView extends View {
    private final float mTextSize;
    private final TextPaint textPaint;
    private final Rect bounds = new Rect();
    private String mText;

    public FileTypeTextView(Context context) {
        this(context, null, 0);
    }

    public FileTypeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FileTypeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 36, context.getResources().getDisplayMetrics());
        textPaint = new TextPaint();
        textPaint.setTextSize(mTextSize);
        textPaint.setTypeface(FontsManager.get().get(context, FontsManager.ROBOTO_BLACK));

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FileTypeTextView, defStyleAttr, 0);
        try {
            textPaint.setColor(a.getColor(R.styleable.FileTypeTextView_textColor, Color.WHITE));
        } finally {
            a.recycle();
        }

        if (isInEditMode()) setExtension("XML");
    }

    public void setFilename(@NonNull String filename) {
        String[] split = filename.split("\\.");
        if (split.length > 0) setExtension(split[split.length - 1]);
        else setExtension(null);
    }

    public void setExtension(@Nullable String ext) {
        if (Objects.equals(mText, ext)) return;
        if (ext == null || ext.length() > 4) ext = "...";
        mText = ext.toUpperCase();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mText != null) {
            int width = MeasureSpec.getSize(widthMeasureSpec);

            textPaint.setTextSize(mTextSize);
            while (textPaint.measureText(mText) >= width) {
                textPaint.setTextSize(textPaint.getTextSize() - 3);
            }

            textPaint.getTextBounds(mText, 0, mText.length(), bounds); // FIXME: Fuck this shit

            widthMeasureSpec = MeasureSpec.makeMeasureSpec(bounds.width() + bounds.left, MeasureSpec.EXACTLY);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(bounds.height() + bounds.bottom, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawText(mText, 0, canvas.getHeight(), textPaint);
    }
}
