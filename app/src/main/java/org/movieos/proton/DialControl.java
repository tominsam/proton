package org.movieos.proton;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;

public class DialControl extends View {
    private transient static final String TAG = DialControl.class.getSimpleName();

    public interface OnDialChangeListener {
        void onDialValueChanged(double value);
    }

    int mGridSpacing;
    //Paint mBackground;
    Paint mNumbers;
    Paint mMajor;
    Paint mMinor;
    Paint mCursor;
    VelocityTracker mVelocityTracker;
    OverScroller mOverScroller;
    ValueAnimator mValueAnimator;

    double mDragOffset = 0; // 0 means middle

    double mWidthMultiplier = 3;

    double mMax = 100; // assumption - 0 is the middle, don't need min
    OnDialChangeListener mListener;

    float mTouchStartX;
    float mTouchStartY;
    float mVelocity;
    double mTouchStartDragOffset = 0;
    double mStep = 1;
    private int mStepsPerMajor = 1;



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
//        mBackground = new Paint();
//        mBackground.setColor(getResources().getColor(R.color.dial_background));

        mMajor = new Paint();
        mMajor.setColor(getResources().getColor(R.color.dial_major));
        mMajor.setStrokeWidth(1);

        mNumbers = new Paint();
        mNumbers.setColor(getResources().getColor(R.color.dial_major));
        mNumbers.setTextAlign(Paint.Align.CENTER);
        mNumbers.setTextSize(getResources().getDimensionPixelSize(R.dimen.dial_font_size));
        mNumbers.setAntiAlias(true);

        mMinor = new Paint();
        mMinor.setStrokeWidth(1);
        mMinor.setColor(getResources().getColor(R.color.dial_minor));

        mCursor = new Paint();
        mCursor.setStrokeWidth(1);
        mCursor.setColor(getResources().getColor(R.color.dial_cursor));

        mOverScroller = new OverScroller(getContext());

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
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //canvas.drawRect(0, 0, getWidth(), getHeight(), mBackground);

        if (mOverScroller.computeScrollOffset()) {
            mDragOffset = mOverScroller.getCurrX();
            broadcastValue();
            ViewCompat.postInvalidateOnAnimation(this);
        }

        // step outwards from the center
        int bottom = getHeight() - getResources().getDimensionPixelSize(R.dimen.dial_bottom);
        int major_top = getResources().getDimensionPixelSize(R.dimen.dial_major_top);
        int minor_top = getResources().getDimensionPixelSize(R.dimen.dial_minor_top);
        int text_bottom = getResources().getDimensionPixelSize(R.dimen.dial_text_bottom);
        for (int i = 0; ; i++) {
            boolean major = i % mStepsPerMajor == 0;
            Paint paint = major ? mMajor : mMinor;
            int top = major ? major_top : minor_top;
            double value = mStep * i;

            int left = valueToScreen(-value);
            int right = valueToScreen(+value);

            if (left >= 0) {
                canvas.drawLine(left, top, left, bottom, paint);
            }
            if (right <= getWidth()) {
                canvas.drawLine(right, top, right, bottom, paint);
            }
            if (major) {
                if (left > 0) {
                    canvas.drawText(String.format("%.0f", -i * mStep), left, getHeight() - text_bottom, mNumbers);
                }
                if (right < getWidth()) {
                    canvas.drawText(String.format("%.0f", i * mStep), right, getHeight() - text_bottom, mNumbers);
                }
            }
            if (left < 0 && right > getWidth()) {
                break;
            }
            if (value >= mMax) {
                break;
            }

        }

        canvas.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight(), mCursor);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    mVelocityTracker.clear();
                }
                mOverScroller.forceFinished(true);
                ViewCompat.postInvalidateOnAnimation(this);
                mVelocityTracker.addMovement(event);
                mTouchStartX = event.getX();
                mTouchStartY = event.getX();
                mTouchStartDragOffset = mDragOffset;
                return true;

            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                mVelocity = mVelocityTracker.getXVelocity();
                float x = event.getX();
                float change = x - mTouchStartX;
                mDragOffset = mTouchStartDragOffset + (int)change;
                mDragOffset = Math.min(mDragOffset, getWidth() * mWidthMultiplier);
                mDragOffset = Math.max(getWidth() * -mWidthMultiplier, mDragOffset);
                broadcastValue();
                invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:
                mOverScroller.forceFinished(true);
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                return true;

            case MotionEvent.ACTION_UP:
                mOverScroller.forceFinished(true);
                mOverScroller.fling((int)mDragOffset, 0, (int)mVelocity, 0, valueToPixel(-mMax), valueToPixel(mMax), 0, 0);
                ViewCompat.postInvalidateOnAnimation(this);
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private double pixelToValue(double pixel) {
        return pixel * mMax / (getWidth() * mWidthMultiplier);
    }

    private int valueToPixel(double value) {
        return (int) (value * (getWidth() * mWidthMultiplier) / mMax);
    }

    private int pixelToScreen(int pixel) {
        return (int) (pixel + getWidth() / 2 + mDragOffset);
    }

    private int valueToScreen(double value) {
        return pixelToScreen(valueToPixel(value));
    }

    public void setValue(double value, int max, double step, int stepsPerMajor) {
        mMax = max;
        mStep = step;
        mStepsPerMajor = stepsPerMajor;
        mWidthMultiplier = (max / step) / 60;
        setValue(value, false);
    }

    public void setValue(double value, boolean animate) {
        mOverScroller.forceFinished(true);
        double target = -valueToPixel(value);
        if (animate) {
            if (mValueAnimator != null) {
                mValueAnimator.cancel();
            }
            mValueAnimator = ValueAnimator.ofFloat((float)mDragOffset, (float)target);
            mValueAnimator.setDuration(200);
            mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mDragOffset = (Float) animation.getAnimatedValue();
                    invalidate();
                    broadcastValue();
                }
            });
            mValueAnimator.setInterpolator(new DecelerateInterpolator());
            mValueAnimator.start();

        } else {
            mDragOffset = target;
            invalidate();
            broadcastValue();
        }
    }

    public void setOnChangeListener(OnDialChangeListener listener) {
        mListener = listener;
    }

    public void broadcastValue() {
        if (mListener != null) {
            mListener.onDialValueChanged(-pixelToValue(mDragOffset));
        }
    }
}
