package com.gianlu.aria2app.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gianlu.aria2app.api.aria2.DownloadWithUpdate;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import java.util.Objects;

public class BitfieldVisualizer extends View {
    private final int padding;
    private final int square;
    private final Paint paint;
    private final Paint border;
    private final Rect rect = new Rect();
    private String bitfield = null;
    private int squares = -1;
    private int[] binary = null;
    private int columns;
    private int rows;
    private int hoff;
    private int numPieces;

    public BitfieldVisualizer(Context context) {
        this(context, null, 0);
    }

    public BitfieldVisualizer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BitfieldVisualizer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int dp4 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        padding = dp4;
        square = dp4 * 4;

        paint = new Paint();
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));

        border = new Paint();
        border.setColor(CommonUtils.resolveAttrAsColor(getContext(), android.R.attr.colorForeground));
        border.setStrokeWidth(dp4 / 4f);
        border.setStyle(Paint.Style.STROKE);
    }

    public static int knownPieces(@NonNull String hex, int num) {
        if (hex.length() == 0) return 0;

        num = (int) Math.ceil(num / 4f);

        int known = 0;
        for (int i = 0; i < num; i++) {
            switch (Character.toLowerCase(hex.charAt(i))) {
                case '0':
                    known += 0;
                    break;
                case '1':
                case '2':
                case '4':
                case '8':
                    known += 1;
                    break;
                case '3':
                case '5':
                case '6':
                case '9':
                case 'a':
                case 'c':
                    known += 2;
                    break;
                case '7':
                case 'b':
                case 'd':
                case 'e':
                    known += 3;
                    break;
                case 'f':
                    known += 4;
                    break;
            }
        }

        return known;
    }

    @NonNull
    private static int[] hexToBinary(String hex, int num) {
        if (hex == null || hex.isEmpty()) return new int[0];

        num = (int) Math.ceil(num / 4f);

        int[] array = new int[num];
        for (int i = 0; i < num; i++) {
            switch (Character.toLowerCase(hex.charAt(i))) {
                case '0':
                    array[i] = 0;
                    break;
                case '1':
                case '2':
                case '4':
                case '8':
                    array[i] = 1;
                    break;
                case '3':
                case '5':
                case '6':
                case '9':
                case 'a':
                case 'c':
                    array[i] = 2;
                    break;
                case '7':
                case 'b':
                case 'd':
                case 'e':
                    array[i] = 3;
                    break;
                case 'f':
                    array[i] = 4;
                    break;
            }
        }

        return array;
    }

    public void setColor(@ColorInt int color) {
        paint.setColor(color);
        invalidate();
    }

    public void setColorRes(@ColorRes int color) {
        setColor(ContextCompat.getColor(getContext(), color));
    }

    public void update(@NonNull DownloadWithUpdate.BigUpdate update) {
        update(update.bitfield, update.numPieces);
    }

    public void update(@NonNull String bitfield, int numPieces) {
        if (Objects.equals(this.bitfield, bitfield)) return;

        this.bitfield = bitfield;
        this.numPieces = numPieces;

        squares = (int) Math.ceil(numPieces / 4f);
        binary = hexToBinary(bitfield, numPieces);

        invalidate();
    }

    private int columns(int width) {
        return width / (square + padding);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (squares == -1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        columns = columns(width);
        rows = (int) Math.ceil((double) squares / (double) columns);

        hoff = (width - columns * (square + padding)) / 4;

        setMeasuredDimension(width, getDefaultSize(rows * (square + padding) + padding, heightMeasureSpec));
    }

    private void calcSquarePos(int row, int column, Rect rect) {
        int px = padding + column * (square + padding) + hoff;
        int py = padding + row * (square + padding);
        rect.left = px;
        rect.top = py;
        rect.right = square + px;
        rect.bottom = square + py;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (binary == null || squares == -1 || binary.length == 0) return;

        int i = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (i >= squares) break;

                if (binary[i] != 0) {
                    if (i == squares - 1 && numPieces % 4 != 0)
                        paint.setAlpha(255 / (numPieces % 4) * binary[i]);
                    else
                        paint.setAlpha(255 / 4 * binary[i]);

                    calcSquarePos(row, column, rect);
                    canvas.drawRect(rect, paint);
                }

                if (i == squares - 1 || i == 0) {
                    calcSquarePos(row, column, rect);
                    canvas.drawRect(rect, border);
                }

                i++;
            }
        }
    }
}
