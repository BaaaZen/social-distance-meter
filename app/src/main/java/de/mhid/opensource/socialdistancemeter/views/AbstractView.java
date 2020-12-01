package de.mhid.opensource.socialdistancemeter.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public abstract class AbstractView extends View {
    protected final Paint pText = new Paint();
    protected final Paint pAxis = new Paint();

    protected final float axisWidth;
    protected final float margin;

    public AbstractView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        axisWidth = 1*getResources().getDisplayMetrics().density;
        margin = 3*getResources().getDisplayMetrics().density;

        pText.setColor(Color.BLACK);
        pText.setTextSize(40);
        pText.setTextAlign(Paint.Align.CENTER);

        pAxis.setColor(Color.BLACK);
        pAxis.setStrokeWidth(axisWidth);
    }
}
