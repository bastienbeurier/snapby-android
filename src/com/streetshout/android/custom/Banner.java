package com.streetshout.android.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class Banner extends ImageView {

    public Banner(Context context) {
        super(context);
    }

    public Banner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Banner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override protected void onMeasure(int widthMeasureSpec,
                                       int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width;
        setMeasuredDimension(width, height);
    }
}