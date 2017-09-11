package com.gianlu.aria2app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.TypedValue;

import java.util.Objects;

public class FileTypeImageView extends AppCompatImageView {
    private final Rect textBounds = new Rect();
    private final int maxWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
    private String ext;
    private Paint textPaint;

    public FileTypeImageView(Context context) {
        super(context);
        init();
    }

    public FileTypeImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FileTypeImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setImageResource(R.drawable.ic_insert_drive_file_black_48dp);
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 9, getResources().getDisplayMetrics()));
        textPaint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Bold.ttf"));

        if (isInEditMode()) setExtension("XML");
    }

    public void setFileName(@NonNull String fileName) {
        String[] split = fileName.split("\\.");
        if (split.length > 0) setExtension(split[split.length - 1]);
        else setExtension(null);
    }

    public void setExtension(@Nullable String ext) {
        if (Objects.equals(this.ext, ext)) return;
        if (ext != null && ext.length() > 4) this.ext = null;
        else this.ext = ext;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (ext != null) {
            boolean nice = false;
            while (!nice) {
                textPaint.getTextBounds(ext, 0, ext.length(), textBounds);
                nice = textBounds.width() < maxWidth;
                if (!nice) textPaint.setTextSize(textPaint.getTextSize() - 1);
            }

            canvas.drawText(ext, 0, ext.length(), (canvas.getWidth() - textBounds.width()) / 2, canvas.getHeight() / 2 + textBounds.height(), textPaint);
        }
    }
}
