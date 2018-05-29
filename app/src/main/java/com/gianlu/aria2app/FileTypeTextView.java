package com.gianlu.aria2app;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;

import com.gianlu.commonutils.FontsManager;

import java.util.Objects;

public class FileTypeTextView extends AppCompatTextView {
    private int mWidth;
    private String mText;

    public FileTypeTextView(Context context) {
        this(context, null, 0);
    }

    public FileTypeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FileTypeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTextSize(36);
        setTypeface(FontsManager.get().get(context, FontsManager.ROBOTO_BLACK));
        setGravity(Gravity.CENTER_VERTICAL);

        if (isInEditMode()) setExtension("XML");
    }

    public void setFilename(@NonNull String filename) {
        String[] split = filename.split("\\.");
        if (split.length > 0) setExtension(split[split.length - 1]);
        else setExtension(null);
    }

    public void setExtension(@Nullable String ext) {
        if (Objects.equals(getText(), ext)) return;
        if (ext == null || ext.length() > 4) ext = "...";
        mText = ext.toUpperCase();
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            mWidth = right - left;
            adjustTextSize();
        }
    }

    private void adjustTextSize() {
        if (mText != null) {
            TextPaint paint = getPaint();
            while (paint.measureText(mText) > mWidth)
                paint.setTextSize(paint.getTextSize() - 1);

            setText(mText);
        }
    }
}
