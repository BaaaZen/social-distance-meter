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
package de.mhid.opensource.socialdistancemeter.utils;

import android.graphics.Color;
import android.util.Log;

import de.mhid.opensource.socialdistancemeter.BuildConfig;

public class Rssi {
    private Rssi() {}

    private static int getColorBetween(int color1, int color2, int percent) {
        int r1 = Color.red(color1);
        int r2 = Color.red(color2);
        int g1 = Color.green(color1);
        int g2 = Color.green(color2);
        int b1 = Color.blue(color1);
        int b2 = Color.blue(color2);
        int a1 = Color.alpha(color1);
        int a2 = Color.alpha(color2);

        int r = r1 + (r2-r1)*percent/100;
        int g = g1 + (g2-g1)*percent/100;
        int b = b1 + (b2-b1)*percent/100;
        int a = a1 + (a2-a1)*percent/100;

        return Color.argb(a, r, g, b);
    }

    public static int getColorForRssi(int rssi, int colorLow, int colorMedium, int colorHigh) {
        if(rssi >= -65) {
            return colorHigh;
        } else if(rssi >= -85) {
            return getColorBetween(colorHigh, colorMedium, (-rssi-65)*100/20);
        } else if(rssi >= -95) {
            return getColorBetween(colorMedium, colorLow, (-rssi-85)*100/10);
        } else {
            return colorLow;
        }
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
