package de.mhid.opensource.cwadetails.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "cwa_token")
public class CwaToken {
  @PrimaryKey(autoGenerate = true)
  public long id;

  @NonNull
  @ColumnInfo(name = "token")
  public String token;

  @NonNull
  @ColumnInfo(name = "mac")
  public String mac;

  @NonNull
  @ColumnInfo(name = "timestamp")
  public Date timestamp;

  @NonNull
  @ColumnInfo(name = "rssi")
  public int rssi;

  @ColumnInfo(name = "lat")
  public Double latitude;

  @ColumnInfo(name = "lon")
  public Double longitude;
}
