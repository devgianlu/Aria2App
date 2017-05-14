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
    private boolean[] binary = null;
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

    private void init() {
        paint = new Paint();
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
    }

    // TODO: Shrink down code
    private boolean[] hexToBinary(String hex, int num) {
        hex = hex.toLowerCase();
        boolean[] array = new boolean[hex.length() * 4];
        for (int i = 0; i < hex.length() * 4; i += 4) {
            switch (hex.charAt(i / 4)) {
                case '0':
                    array[i] = false;
                    array[i + 1] = false;
                    array[i + 2] = false;
                    array[i + 3] = false;
                    break;
                case '1':
                    array[i] = false;
                    array[i + 1] = false;
                    array[i + 2] = false;
                    array[i + 3] = true;
                    break;
                case '2':
                    array[i] = false;
                    array[i + 1] = false;
                    array[i + 2] = true;
                    array[i + 3] = false;
                    break;
                case '3':
                    array[i] = false;
                    array[i + 1] = false;
                    array[i + 2] = true;
                    array[i + 3] = true;
                    break;
                case '4':
                    array[i] = false;
                    array[i + 1] = true;
                    array[i + 2] = false;
                    array[i + 3] = false;
                    break;
                case '5':
                    array[i] = false;
                    array[i + 1] = true;
                    array[i + 2] = false;
                    array[i + 3] = true;
                    break;
                case '6':
                    array[i] = false;
                    array[i + 1] = true;
                    array[i + 2] = true;
                    array[i + 3] = false;
                    break;
                case '7':
                    array[i] = false;
                    array[i + 1] = true;
                    array[i + 2] = true;
                    array[i + 3] = true;
                    break;
                case '8':
                    array[i] = true;
                    array[i + 1] = false;
                    array[i + 2] = false;
                    array[i + 3] = false;
                    break;
                case '9':
                    array[i] = true;
                    array[i + 1] = false;
                    array[i + 2] = false;
                    array[i + 3] = true;
                    break;
                case 'a':
                    array[i] = true;
                    array[i + 1] = false;
                    array[i + 2] = true;
                    array[i + 3] = false;
                    break;
                case 'b':
                    array[i] = true;
                    array[i + 1] = false;
                    array[i + 2] = true;
                    array[i + 3] = true;
                    break;
                case 'c':
                    array[i] = true;
                    array[i + 1] = true;
                    array[i + 2] = false;
                    array[i + 3] = false;
                    break;
                case 'd':
                    array[i] = true;
                    array[i + 1] = true;
                    array[i + 2] = false;
                    array[i + 3] = true;
                    break;
                case 'e':
                    array[i] = true;
                    array[i + 1] = true;
                    array[i + 2] = true;
                    array[i + 3] = false;
                    break;
                case 'f':
                    array[i] = true;
                    array[i + 1] = true;
                    array[i + 2] = true;
                    array[i + 3] = true;
                    break;
            }
        }

        return Arrays.copyOfRange(array, 0, num);
    }

    public void setColor(@ColorRes int color) {
        paint.setColor(ContextCompat.getColor(getContext(), color));
        invalidate();
    }

    public void update(Download download) {
        if (Objects.equals(bitfield, download.bitfield)) return;

        bitfield = download.bitfield;
        pieces = download.numPieces;
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
                if (i < pieces && binary[i]) {
                    int px = padding + column * (square + padding) + hoff;
                    int py = padding + row * (square + padding);
                    canvas.drawRect(px, py, square + px, square + py, paint);
                }
                i++;
            }
        }
    }
}
