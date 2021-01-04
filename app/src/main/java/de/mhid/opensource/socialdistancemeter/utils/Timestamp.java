/*
Social Distance Meter - An app to analyze and rate your social distancing behavior
Copyright (C) 2021  Mirko Hansen (baaazen@gmail.com)

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

import java.util.Date;
import java.util.TimeZone;

public class Timestamp {
    public static Timestamp getCurrentTimestamp() {
        return new Timestamp(new Date());
    }

    private final Date timestamp;
    private final int utcOffset;

    private Timestamp(Date t) {
        this.timestamp = t;
        this.utcOffset = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();
    }

    public long getRollingTimestamp() {
        return getUTCTimestamp().getTime() / (10 * 60 * 1000);
    }

    public Date getLocalTimestamp() {
        return timestamp;
    }
    public Date getUTCTimestamp() {
        return new Date(getLocalTimestamp().getTime() - utcOffset);
    }
}
