package org.movieos.proton;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GridOverlay extends View {
    private transient static final String TAG = GridOverlay.class.getSimpleName();

    int mGridSpacing;
    Paint mGridPaint;

    public GridOverlay(Context context) {
        super(context);
        init();
    }

    public GridOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        mGridSpacing = getContext().getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        mGridPaint = new Paint();
        mGridPaint.setColor(getContext().getResources().getColor(R.color.grid_color));
        mGridPaint.setStrokeWidth(1);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 0;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        // I want to be square
        int desiredHeight = width;

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int x = mGridSpacing; x < canvas.getWidth(); x += mGridSpacing) {
            canvas.drawLine(x, 0, x, canvas.getHeight(), mGridPaint);
        }
        for (int y = mGridSpacing; y < canvas.getHeight(); y += mGridSpacing) {
            canvas.drawLine(0, y, canvas.getWidth(), y, mGridPaint);
        }
    }
}
