package com.vfdev.gettingthingsdonemusicapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Waveform view draw image and progress rectangle above
 */
public class WaveformView extends ImageView {


    private double mProgress = 0.0;
    private int mProgressColor;

    private Paint mPaint;
    private Rect mRect;
    private Path mPath;

    public WaveformView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attributeSet,
                R.styleable.WaveformView,
                0, 0);
//        int progressColor;
        try {
            mProgressColor = a.getColor(R.styleable.WaveformView_progressColor, Color.argb(122, 122, 122, 122));
        } finally {
            a.recycle();
        }

        mRect = new Rect();
        mPath = new Path();

        mPaint = new Paint();
        mPaint.setColor(mProgressColor);

    }

//    public double getProgress() {
//        return mProgress;
//    }

    public void setProgress(double progress) {
        mProgress = progress;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int wp = (int) (getWidth()*mProgress);
        int h = getHeight();

        // draw the border:
        mPaint.setColor(Color.argb(122, 0, 0, 0));
        mPath.addRect(0,0,getWidth(),h, Path.Direction.CCW);
        canvas.drawPath(mPath, mPaint);

        // draw progress rectangle
        mRect.set(0,0,wp,h);
        mPaint.setColor(mProgressColor);
        canvas.drawRect(mRect, mPaint);

    }


}