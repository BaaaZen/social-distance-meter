package de.mhid.opensource.cwadetails.database;

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
