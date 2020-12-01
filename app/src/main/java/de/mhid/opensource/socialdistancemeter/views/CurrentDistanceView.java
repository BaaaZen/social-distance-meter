package de.mhid.opensource.socialdistancemeter.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.services.BleScanService;
import de.mhid.opensource.socialdistancemeter.utils.Rssi;

public class CurrentDistanceView extends AbstractView {
    final float arrowSize = 5*getResources().getDisplayMetrics().density;
    final Drawable userSelf;
    final Drawable user1;
    final Drawable user2;
    final Drawable user3;
    final byte[] drawDistanceCount = new byte[101];
    final int[] drawDistanceColor = new int[101];

    private List<BleScanService.CwaScanResult> sortedScanResults;

    public CurrentDistanceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        userSelf = VectorDrawableCompat.create(getContext().getResources(), R.drawable.round_person_24, null);
        user1 = VectorDrawableCompat.create(getContext().getResources(), R.drawable.round_person_24, null);
        user2 = VectorDrawableCompat.create(getContext().getResources(), R.drawable.round_people_24, null);
        user3 = VectorDrawableCompat.create(getContext().getResources(), R.drawable.round_groups_24, null);

        update();
    }

    private void drawDrawable(Canvas canvas, Drawable drawable, float x, float y) {
        canvas.translate(x, y);
        drawable.draw(canvas);
        canvas.translate(-x, -y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // now paint
        int width = getWidth();
        int height = getHeight();

        final float iconSizeMax = (float)width/6;
        final float xAxisYCoordinate = height - (margin + (pText.getFontMetrics().bottom - pText.getFontMetrics().ascent) + arrowSize + axisWidth);
        final float iconSizeSelf = Math.min(iconSizeMax, xAxisYCoordinate);
        final float iconSizeMin = iconSizeSelf/2;

        // draw self icon
        userSelf.setBounds(0, 0, (int)iconSizeMax, (int)iconSizeMax);
        userSelf.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
        drawDrawable(canvas, userSelf, 0, xAxisYCoordinate - iconSizeSelf);

        // draw x-axis
        canvas.drawLine(iconSizeSelf, xAxisYCoordinate, width, xAxisYCoordinate, pAxis);
        canvas.drawLine(width, xAxisYCoordinate, width-arrowSize, xAxisYCoordinate-arrowSize, pAxis);
        canvas.drawLine(width, xAxisYCoordinate, width-arrowSize, xAxisYCoordinate+arrowSize, pAxis);

        // draw x-axis label
        String distanceCaption = getResources().getString(R.string.card_current_distance);
        canvas.drawText(distanceCaption, (float)width/2, xAxisYCoordinate - pText.getFontMetrics().ascent + arrowSize, pText);

        float xPerPercent = (width - iconSizeSelf - iconSizeMin)/100;
        List<BleScanService.CwaScanResult> sortedResults;
        synchronized (this) {
            sortedResults = sortedScanResults;
        }
        // reset counter
        for(int i=0; i<drawDistanceCount.length; i++) {
            drawDistanceCount[i] = 0;
        }
        int lastDistance = -1;
        float lastPosX = 0;
        for(BleScanService.CwaScanResult item : sortedResults) {
            int distancePercent = Rssi.getDistancePercentForRssi(item.getRssi());

            int iconColor = Rssi.getColorForRssi(item.getRssi());
            float iconSize = (100-distancePercent)*(iconSizeSelf-iconSizeMin)/100+iconSizeMin;
            float posX = iconSizeSelf + distancePercent*xPerPercent;
            if(posX >= lastPosX) {
                lastDistance = distancePercent;
                lastPosX = posX + iconSize;
            }
            drawDistanceCount[lastDistance] = (byte)Math.min(drawDistanceCount[lastDistance]+1, 3);
            drawDistanceColor[lastDistance] = iconColor;
        }

        for(int i=drawDistanceCount.length-1; i>=0; i--) {
            if(drawDistanceCount[i] == 0) continue;

            float iconSize = (100-i)*(iconSizeSelf-iconSizeMin)/100+iconSizeMin;

            // draw people
            Drawable userIcon;
            if(drawDistanceCount[i] == 3) {
                userIcon = user3;
            } else if(drawDistanceCount[i] == 2) {
                userIcon = user2;
            } else {
                userIcon = user1;
            }
            userIcon.setBounds(0, 0, (int)iconSize, (int)iconSize);
            userIcon.setColorFilter(drawDistanceColor[i], PorterDuff.Mode.MULTIPLY);
            drawDrawable(canvas, userIcon, iconSizeSelf + i*xPerPercent, xAxisYCoordinate - iconSizeSelf/2 - iconSize/2);
        }
    }

    public void update() {
        HashMap<String, BleScanService.CwaScanResult> scanResults = BleScanService.getScanResults();
        ArrayList<BleScanService.CwaScanResult> sortedResults;
        if(scanResults != null) {
            sortedResults = new ArrayList<>(scanResults.values());
            Collections.sort(sortedResults, Collections.reverseOrder());
        } else {
            sortedResults = new ArrayList<>();
        }
        synchronized (this) {
            sortedScanResults = sortedResults;
        }

        this.invalidate();
    }
}
