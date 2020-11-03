package de.mhid.opensource.cwadetails.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;

public class HistoryGraphView extends View {
    public HistoryGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float margin = 3*getResources().getDisplayMetrics().density;
        float markerLen = 3*getResources().getDisplayMetrics().density;
        float axisWidth = 1*getResources().getDisplayMetrics().density;

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        Paint pText = new Paint();
        pText.setColor(Color.BLACK);
        pText.setTextSize(40);
        pText.setTextAlign(Paint.Align.CENTER);

        float yAxis = height - (margin + (pText.getFontMetrics().bottom - pText.getFontMetrics().ascent) + markerLen + axisWidth);

        Paint pAxis = new Paint();
        pAxis.setColor(Color.BLACK);
        pAxis.setStrokeWidth(axisWidth);

        // draw x-axis
        canvas.drawLine(0, yAxis, width, yAxis, pAxis);

        // calc 5 minute slot width
        float slotWidth = width / (6*12);

        Date currentDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        float firstShift = cal.get(Calendar.MINUTE)/5*slotWidth;
        float currentMarkerX = width-firstShift + 12*slotWidth;
        int currentHour = (cal.get(Calendar.HOUR_OF_DAY)+1) % 24;
        while(currentMarkerX > 0) {
            canvas.drawLine(currentMarkerX, yAxis, currentMarkerX, yAxis + markerLen, pAxis);

            String timeStr = Integer.toString(currentHour) + ":00";
            float timeStrWidth = pText.measureText(timeStr);
            float timeStrBorderShift = 0;
            if(currentMarkerX - timeStrWidth/2 < margin) {
                timeStrBorderShift = margin - (currentMarkerX - timeStrWidth/2);
            }
            canvas.drawText(timeStr, currentMarkerX + timeStrBorderShift, yAxis + markerLen - pText.getFontMetrics().ascent, pText);

            currentHour = (currentHour - 1 + 24) % 24;
            currentMarkerX = currentMarkerX - 12*slotWidth;
        }
    }
}
