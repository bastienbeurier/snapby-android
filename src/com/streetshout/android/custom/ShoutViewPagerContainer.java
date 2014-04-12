package com.streetshout.android.custom;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
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

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mPager.dispatchTouchEvent(ev);
    }
}
