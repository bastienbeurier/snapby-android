package com.streetshout.android.custom;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by bastien on 4/11/14.
 */
public class ShoutViewPagerContainer extends FrameLayout {
    private ViewPager mPager;

    public ShoutViewPagerContainer(Context context) {
        super(context);
    }

    public ShoutViewPagerContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShoutViewPagerContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        mPager = (ViewPager) getChildAt(0);
    }

    private Point mCenter = new Point();
    private Point mInitialTouch = new Point();


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mCenter.x = w / 2;
        mCenter.y = h / 2;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitialTouch.x = (int)ev.getX();
                mInitialTouch.y = (int)ev.getY();
            default:
                ev.offsetLocation(mCenter.x - mInitialTouch.x, mCenter.y - mInitialTouch.y);
                break;
        }

        return mPager.dispatchTouchEvent(ev);
    }
}
