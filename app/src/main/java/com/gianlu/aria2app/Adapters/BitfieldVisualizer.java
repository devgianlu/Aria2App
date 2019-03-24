package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.R;
import com.gianlu.commonutils.CommonUtils;

import java.util.Arrays;
import java.util.Objects;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class BitfieldVisualizer extends View {
    private final int padding;
    private final int square;
    private final Paint paint;
    private final Paint border;
    private final Rect rect = new Rect();
    private String bitfield = null;
    private int pieces = -1;
    private int[] binary = null;
    private int columns;
    private int rows;
    private int hoff;

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

    @Nullable
    private static int[] hexToBinary(@Nullable String hex, int num) {
        if (hex == null) return null;
        int[] array = new int[hex.length()];
        for (int i = 0; i < hex.length(); i++) {
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

        return Arrays.copyOfRange(array, 0, num);
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
        pieces = numPieces / 4;
        binary = hexToBinary(bitfield, pieces);

        invalidate();
    }

    private int columns(int width) {
        return width / (square + padding);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (pieces == -1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        columns = columns(width);
        rows = (int) Math.ceil((double) pieces / (double) columns);

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
        if (binary == null || pieces == -1) return;

        int i = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (i < pieces && binary[i] != 0) {
                    paint.setAlpha(255 / 4 * binary[i]);
                    calcSquarePos(row, column, rect);
                    canvas.drawRect(rect, paint);
                }

                if ((row == 0 && column == 0) || (row == rows - 1 && column == columns - 1)) {
                    calcSquarePos(row, column, rect);
                    canvas.drawRect(rect, border);
                }

                i++;
            }
        }
    }
}
