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
