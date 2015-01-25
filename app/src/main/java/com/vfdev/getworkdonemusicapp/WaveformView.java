package com.vfdev.getworkdonemusicapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Waveform view draw image and progress rectangle above
 */
public class WaveformView extends ImageView {


    private double mProgress = 0.0;
//    private int mProgressColor;

    private Paint mPaint;
    private Rect mRect;

    public WaveformView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attributeSet,
                R.styleable.WaveformView,
                0, 0);
        int progressColor;
        try {
            progressColor = a.getColor(R.styleable.WaveformView_progressColor, Color.argb(122, 122, 122, 122));
        } finally {
            a.recycle();
        }

        mRect = new Rect();

        mPaint = new Paint();
        mPaint.setColor(progressColor);

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
        mRect.set(0,0,wp,h);
        canvas.drawRect(mRect, mPaint);
    }


}