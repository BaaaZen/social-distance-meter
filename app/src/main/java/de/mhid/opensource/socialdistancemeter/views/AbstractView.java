/*
Social Distance Meter - An app to analyze and rate your social distancing behavior
Copyright (C) 2020  Mirko Hansen (baaazen@gmail.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package de.mhid.opensource.socialdistancemeter.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import de.mhid.opensource.socialdistancemeter.R;

public abstract class AbstractView extends View {
    protected final Paint pText = new Paint();
    protected final Paint pAxis = new Paint();

    protected final float axisWidth;
    protected final float margin;

    protected final int colorDraw;
    protected final int colorFont;
    protected final int colorWarningLow;
    protected final int colorWarningMedium;
    protected final int colorWarningHigh;

    public AbstractView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        axisWidth = 1*getResources().getDisplayMetrics().density;
        margin = 3*getResources().getDisplayMetrics().density;

        TypedArray ta = getContext().obtainStyledAttributes(new int[] {R.attr.colorDraw, R.attr.colorFont, R.attr.colorWarningLow, R.attr.colorWarningMedium, R.attr.colorWarningHigh});
        colorDraw = ta.getColor(0, 0);
        colorFont = ta.getColor(1, 0);
        colorWarningLow = ta.getColor(2, 0);
        colorWarningMedium = ta.getColor(3, 0);
        colorWarningHigh = ta.getColor(4, 0);
        ta.recycle();

        pText.setColor(colorFont);
        pText.setTextSize(40);
        pText.setTextAlign(Paint.Align.CENTER);

        pAxis.setColor(colorDraw);
        pAxis.setStrokeWidth(axisWidth);
    }
}
