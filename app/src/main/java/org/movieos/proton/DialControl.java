package org.movieos.proton;

import android.content.Context;
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

    double mValue = 0;
    double mMax = 1;
    double mMin = -1;
    OnDialChangeListener mListener;

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
        mGridSpacing = getContext().getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        mBackground = new Paint();
        mBackground.setColor(getContext().getResources().getColor(R.color.dial_background));
        mMajor = new Paint();
        mMajor.setColor(getContext().getResources().getColor(R.color.dial_major));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = 0;
        int desiredHeight = getContext().getResources().getDimensionPixelSize(R.dimen.dial_height);

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
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), mBackground);

        int abstractWidth = getWidth() * 10;
        double neutralValue = mMin + (mMax - mMin) / 2;
        double relativeValue = (mValue - mMin) / (mMax - mMin);

        int middle = (int)(getWidth() * relativeValue);

        int x = middle;
        canvas.drawLine(x, 0, x, getHeight(), mMajor);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            default:
                return super.onTouchEvent(event);
        }
    }

    public double getValue() {
        return mValue;
    }

    public void setValue(double value) {
        mValue = value;
    }

    public void setRange(double min, double max) {
        mMin = min;
        mMax = max;
    }

    public void setOnChangeListener(OnDialChangeListener listener) {
        mListener = listener;
    }
}
