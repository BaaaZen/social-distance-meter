package de.mhid.opensource.cwadetails.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import de.mhid.opensource.cwadetails.database.CwaTokenStatistics;
import de.mhid.opensource.cwadetails.database.Database;

public class HistoryGraphView extends View {
    private List<CwaTokenStatistics> statisticsList = null;

    private Paint pBlockDiagram;
    private Paint pText;
    private Paint pAxis;

    float margin;
    float markerLen;
    float axisWidth;
    float blockSpacing;

    public HistoryGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        margin = 3*getResources().getDisplayMetrics().density;
        markerLen = 3*getResources().getDisplayMetrics().density;
        axisWidth = 1*getResources().getDisplayMetrics().density;
        blockSpacing = 1*getResources().getDisplayMetrics().density;

        pBlockDiagram = new Paint();

        pText = new Paint();
        pText.setColor(Color.BLACK);
        pText.setTextSize(40);
        pText.setTextAlign(Paint.Align.CENTER);

        pAxis = new Paint();
        pAxis.setColor(Color.BLACK);
        pAxis.setStrokeWidth(axisWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // now paint
        int width = getWidth();
        int height = getHeight();

        float yAxis = height - (margin + (pText.getFontMetrics().bottom - pText.getFontMetrics().ascent) + markerLen + axisWidth);

        // draw x-axis
        canvas.drawLine(0, yAxis, width, yAxis, pAxis);

        // calc 5 minute slot width
        float slotWidth = (float)width / (6*6);

        Date currentDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        float firstShift = (float)cal.get(Calendar.MINUTE)/10*slotWidth;
        float currentMarkerX = width-firstShift + 6*slotWidth;
        int currentHour = (cal.get(Calendar.HOUR_OF_DAY)+1) % 24;
        while(currentMarkerX > 0) {
            canvas.drawLine(currentMarkerX, yAxis, currentMarkerX, yAxis + markerLen, pAxis);

            String timeStr = currentHour + ":00";
            float timeStrWidth = pText.measureText(timeStr);
            float timeStrBorderShift = 0;
            if(currentMarkerX - timeStrWidth/2 < margin) {
                timeStrBorderShift = margin - (currentMarkerX - timeStrWidth/2);
            }
            canvas.drawText(timeStr, currentMarkerX + timeStrBorderShift, yAxis + markerLen - pText.getFontMetrics().ascent, pText);

            currentHour = (currentHour - 1 + 24) % 24;
            currentMarkerX = currentMarkerX - 6*slotWidth;
        }

        // get max contacts per rolling timestamp
        List<CwaTokenStatistics> statisticsList = getStatisticsList();
        int maxItemsPerRollingTimestamp = 0;
        if(statisticsList != null) {
            long currentRollingTimestamp = 0;
            HashSet<String> mac = new HashSet<>();
            for (CwaTokenStatistics statistics : statisticsList) {
                if (statistics.rollingTimestamp != currentRollingTimestamp) {
                    if (currentRollingTimestamp != 0 && mac.size() > maxItemsPerRollingTimestamp)
                        maxItemsPerRollingTimestamp = mac.size();
                    currentRollingTimestamp = statistics.rollingTimestamp;
                    mac.clear();
                }
                mac.add(statistics.mac);
            }
            if (currentRollingTimestamp != 0 && mac.size() > maxItemsPerRollingTimestamp)
                maxItemsPerRollingTimestamp = mac.size();
        }

        if(maxItemsPerRollingTimestamp == 0) return;

        float rollingBlockShift = (float)(cal.get(Calendar.MINUTE)%10)/10*slotWidth;
        long startRollingTimestamp = currentDate.getTime()/(1000*60*10);
        long currentRollingTimestamp = 0;
        ArrayList<Integer> colors = new ArrayList<>();
        HashSet<String> mac = new HashSet<>();
        float blockX = 0;
        int alpha = 255;
        for(CwaTokenStatistics statistics : statisticsList) {
            if (statistics.rollingTimestamp != currentRollingTimestamp) {
                if(currentRollingTimestamp != 0) paintBlock(canvas, blockX+blockSpacing, yAxis-axisWidth/2 - (yAxis-axisWidth/2)/maxItemsPerRollingTimestamp*mac.size(), blockX+slotWidth-blockSpacing, yAxis-axisWidth/2, colors, alpha);

                currentRollingTimestamp = statistics.rollingTimestamp;
                colors.clear();
                mac.clear();

                blockX = (width-rollingBlockShift) - (startRollingTimestamp - currentRollingTimestamp)*slotWidth;

                if(currentRollingTimestamp == startRollingTimestamp) {
                    alpha = (int)(rollingBlockShift/slotWidth*255);
                } else if(blockX < 0) {
                    alpha = (int)((slotWidth-(-blockX))/slotWidth*255);
                } else {
                    alpha = 255;
                }
            }
            mac.add(statistics.mac);
            colors.add(getColorForRssi(statistics.rssi));
        }
        paintBlock(canvas, blockX+blockSpacing, yAxis-axisWidth/2 - (yAxis-axisWidth/2)/maxItemsPerRollingTimestamp*mac.size(), blockX+slotWidth-blockSpacing, yAxis-axisWidth/2, colors, alpha);
    }

    private int getColorForRssi(int rssi) {
        int r;
        int g;
        if(rssi >= -65) {
            r = 255;
            g = 0;
        } else if(rssi >= -85) {
            r = 255;
            g = 255 - (rssi-(-85))*255/20;
        } else if(rssi >= -95) {
            r = (rssi-(-95))*255/10;
            g = 255;
        } else {
            r = 0;
            g = 255;
        }

        return Color.rgb(r, g, 0);
    }

    private void paintBlock(Canvas canvas, float left, float top, float right, float bottom, ArrayList<Integer> colors, int alpha) {
        if(colors.isEmpty()) return;
        if(colors.size() == 1) {
            pBlockDiagram.setColor(colors.get(0));
        } else {
            int[] colorArray = new int[colors.size()];
            for (int i = 0; i < colors.size(); i++) {
                colorArray[i] = colors.get(i);
            }

            pBlockDiagram.setShader(new LinearGradient(left, bottom, left, top, colorArray, null, Shader.TileMode.REPEAT));
        }
        pBlockDiagram.setAlpha(alpha);
        canvas.drawRect(left, top, right, bottom, pBlockDiagram);

//        float blockHeight = (bottom-top)/colors.size();
//        for(Integer c : colors) {
//            pBlockDiagram.setColor(c);
//            pBlockDiagram.setAlpha(alpha);
//            canvas.drawRect(left, bottom-blockHeight, right, bottom, pBlockDiagram);
//            bottom -= blockHeight;
//        }
    }

    public void update() {
        Database db = Database.getInstance(getContext());

        db.runAsync(new Runnable() {
            @Override
            public void run() {
                long currentRollingTimestamp = new Date().getTime()/(1000*60*10);
                List<CwaTokenStatistics> statisticsList = db.cwaDatabase().cwaToken().getStatistics(currentRollingTimestamp-6*6, currentRollingTimestamp);
                setStatisticsList(statisticsList);

                HistoryGraphView.this.invalidate();
            }
        });
    }

    private synchronized void setStatisticsList(List<CwaTokenStatistics> statisticsList) {
        this.statisticsList = statisticsList;
    }

    private synchronized List<CwaTokenStatistics> getStatisticsList() {
        return statisticsList;
    }
}
