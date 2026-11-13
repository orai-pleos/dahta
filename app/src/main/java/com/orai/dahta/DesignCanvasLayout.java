package com.orai.dahta;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/** Scales the fixed Figma artboard uniformly without clipping on smaller displays. */
public final class DesignCanvasLayout extends FrameLayout {
    public DesignCanvasLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        float density = getResources().getDisplayMetrics().density;
        View canvas = getChildAt(0);
        LayoutParams params = (LayoutParams) canvas.getLayoutParams();
        int canvasWidth = params.width > 0 ? params.width : Math.round(2560 * density);
        int canvasHeight = params.height > 0 ? params.height : Math.round(1446 * density);
        canvas.measure(MeasureSpec.makeMeasureSpec(canvasWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(canvasHeight, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        View canvas = getChildAt(0);
        int width = canvas.getMeasuredWidth();
        int height = canvas.getMeasuredHeight();
        int availableWidth = Math.max(0, getWidth() - getPaddingLeft() - getPaddingRight());
        int availableHeight = Math.max(0, getHeight() - getPaddingTop() - getPaddingBottom());
        float scale = Math.min((float) availableWidth / width, (float) availableHeight / height);
        canvas.layout(0, 0, width, height);
        canvas.setPivotX(0);
        canvas.setPivotY(0);
        canvas.setScaleX(scale);
        canvas.setScaleY(scale);
        canvas.setTranslationX(getPaddingLeft() + (availableWidth - width * scale) / 2f);
        canvas.setTranslationY(getPaddingTop() + (availableHeight - height * scale) / 2f);
    }
}
