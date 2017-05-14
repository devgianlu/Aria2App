package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.R;

import java.util.Arrays;
import java.util.Objects;

public class BitfieldVisualizer extends View {
    private String bitfield = null;
    private int pieces = -1;
    private int[] binary = null;
    private Paint paint;
    private int padding = 12;
    private int square = 48;
    private int columns;
    private int rows;
    private int hoff;


    public BitfieldVisualizer(Context context) {
        super(context);
        init();
    }

    public BitfieldVisualizer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BitfieldVisualizer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private static int[] hexToBinary(String hex, int num) {
        hex = hex.toLowerCase();
        int[] array = new int[hex.length()];
        for (int i = 0; i < hex.length(); i++) {
            switch (hex.charAt(i)) {
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

    private void init() {
        paint = new Paint();
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
    }

    public void setColor(@ColorRes int color) {
        paint.setColor(ContextCompat.getColor(getContext(), color));
        invalidate();
    }

    public void update(Download download) {
        if (Objects.equals(bitfield, download.bitfield)) return;

        bitfield = download.bitfield;
        pieces = download.numPieces / 4;
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

    @Override
    protected void onDraw(Canvas canvas) {
        if (binary == null || pieces == -1) return;

        int i = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (i < pieces && binary[i] != 0) {
                    paint.setAlpha(255 / 4 * binary[i]);
                    int px = padding + column * (square + padding) + hoff;
                    int py = padding + row * (square + padding);
                    canvas.drawRect(px, py, square + px, square + py, paint);
                }
                i++;
            }
        }
    }
}
