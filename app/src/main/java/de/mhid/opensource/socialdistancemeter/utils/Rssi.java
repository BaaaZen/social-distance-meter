package de.mhid.opensource.socialdistancemeter.utils;

import android.graphics.Color;

public class Rssi {
    private Rssi() {}

    public static int getColorForRssi(int rssi) {
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

    public static int getDistancePercentForRssi(int rssi) {
        if(rssi >= -65) {
            // very close -> distance 0
            return 0;
        } else if(rssi >= -105) {
            // linear distance (near) 0 --> 100 (far)
            return ((-rssi)-65)*100/40;
        } else {
            // very far away -> distance 100
            return 100;
        }
    }
}
