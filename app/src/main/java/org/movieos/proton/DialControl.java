package org.movieos.proton;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DialControl extends View {
    private transient static final String TAG = DialControl.class.getSimpleName();

    public interface OnDialChangeListener {
        void onDialValueChanged(double value);
    }

    int mGridSpacing;
    Paint mBackground;
    Paint mMajor;
    Paint mMinor;
    Paint mCursor;

    Bitmap mFullRange;
    double mDragOffset = 0;

    double mMax = 1;
    double mMin = -1;
    OnDialChangeListener mListener;

    float mTouchStartX;
    float mTouchStartY;
    double mTouchStartValue;
    double mTouchStartDragOffset;

    public DialControl(Context context) {
        super(context);
        init();
    }

    public DialControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DialControl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        mGridSpacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        mBackground = new Paint();
        mBackground.setColor(getResources().getColor(R.color.dial_background));
        mMajor = new Paint();
        mMajor.setColor(getResources().getColor(R.color.dial_major));
        mMajor.setTextAlign(Paint.Align.CENTER);
        mMajor.setTextSize(getResources().getDimensionPixelSize(R.dimen.dial_font_size));
        mMajor.setAntiAlias(true);

        mMinor = new Paint();
        mMinor.setColor(getResources().getColor(R.color.dial_minor));
        mCursor = new Paint();
        mCursor.setColor(getResources().getColor(R.color.dial_cursor));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 0;
        int desiredHeight = getResources().getDimensionPixelSize(R.dimen.dial_height);

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
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mFullRange = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), mBackground);

        // step outwards from the center
        int center = (int)(mDragOffset + getWidth() / 2);
        int pixelStep = getWidth() * 10 / 1000;
        double valueStep = (mMax - mMin) / 1000;
        for (int i = 0; i * pixelStep <= getWidth() * 5; i++) {
            boolean major = i % 10 == 0;
            Paint paint = major ? mMajor : mMinor;
            int top = major ? getResources().getDimensionPixelSize(R.dimen.dial_major_top) : getResources().getDimensionPixelSize(R.dimen.dial_minor_top);
            int bottom = getHeight() - getResources().getDimensionPixelSize(R.dimen.dial_bottom);

            canvas.drawLine(center + i * pixelStep, top, center + i * pixelStep, bottom, paint);
            canvas.drawLine(center - i * pixelStep, top, center - i * pixelStep, bottom, paint);
            if (major) {
                canvas.drawText(String.format("%.2f", i * valueStep), center + i * pixelStep, getHeight(), paint);
                canvas.drawText(String.format("%.2f", -i * valueStep), center - i * pixelStep, getHeight(), paint);
            }
        }

        canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight(), mCursor);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchStartX = event.getX();
                mTouchStartY = event.getX();
                mTouchStartDragOffset = mDragOffset;
                return true;

            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float change = x - mTouchStartX;
                mDragOffset = mTouchStartDragOffset + change;
                mDragOffset = Math.min(mDragOffset, getWidth() * 5);
                mDragOffset = Math.max(mDragOffset, getWidth() * -5);
                double value = (mMax - mMin) * (mDragOffset / (getWidth() * 10) + 0.5) + mMin;
                broadcastValue(value);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            default:
                return super.onTouchEvent(event);
        }
    }

    public void setValue(double value) {
        mDragOffset = getWidth() * 5 * (value / mMax);
        invalidate();
        broadcastValue(value);
    }

    public void setRange(double min, double max) {
        mMin = min;
        mMax = max;
    }

    public void setOnChangeListener(OnDialChangeListener listener) {
        mListener = listener;
    }

    public void broadcastValue(double value) {
        if (mListener != null) {
            mListener.onDialValueChanged(value);
        }
    }
}
