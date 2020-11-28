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
package de.mhid.opensource.socialdistancemeter.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "cwa_token",
        foreignKeys = @ForeignKey(entity = CwaDiagKey.class, parentColumns = "id", childColumns = "diagkey_id", onDelete = ForeignKey.SET_NULL),
        indices = { @Index("diagkey_id") }
)
public class CwaToken {
  @PrimaryKey(autoGenerate = true)
  public long id;

  @ColumnInfo(name = "diagkey_id")
  public Long diagKey = null;

  @NonNull
  @ColumnInfo(name = "token")
  public String token;

  @NonNull
  @ColumnInfo(name = "mac")
  public String mac;

  @NonNull
  @ColumnInfo(name = "rolling_timestamp")
  public long rollingTimestamp;

  @NonNull
  @ColumnInfo(name = "local_timestamp")
  public Date localTimestamp;

  @NonNull
  @ColumnInfo(name = "utc_timestamp")
  public Date utcTimestamp;

  @NonNull
  @ColumnInfo(name = "rssi")
  public int rssi;

  @ColumnInfo(name = "lat")
  public Double latitude;

  @ColumnInfo(name = "lon")
  public Double longitude;
}
