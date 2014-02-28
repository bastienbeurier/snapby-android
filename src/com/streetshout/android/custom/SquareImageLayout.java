package com.streetshout.android.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SquareImageLayout extends LinearLayout {
    public SquareImageLayout(Context context) {
        super(context);
    }

    public SquareImageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }
}