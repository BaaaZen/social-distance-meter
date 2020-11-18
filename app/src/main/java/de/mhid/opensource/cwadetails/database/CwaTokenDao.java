package de.mhid.opensource.cwadetails.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

@Dao
public interface CwaTokenDao {
  @Insert
  long insert(CwaToken token);

  @Update
  Void update(List<CwaToken> tokens);

  @Query("SELECT * FROM cwa_token")
  List<CwaToken> getAll();

  @Query("SELECT MIN(rolling_timestamp) as minRollingTimestamp FROM cwa_token")
  CwaTokenMinRollingTimestamp getMinRollingTimestamp();

  @Query("SELECT mac, rssi, rolling_timestamp as rollingTimestamp FROM cwa_token WHERE rollingTimestamp >= :minRollingTimestamp AND rollingTimestamp <= :maxRollingTimestamp ORDER BY rolling_timestamp DESC, rssi ASC, mac ASC")
  List<CwaTokenStatistics> getStatistics(long minRollingTimestamp, long maxRollingTimestamp);

  @Query("SELECT * FROM cwa_token WHERE diagkey_id IS NULL AND rolling_timestamp >= :minRollingTimestamp AND rolling_timestamp < :maxRollingTimestamp ORDER BY rolling_timestamp ASC, mac ASC, token ASC")
  List<CwaToken> getRollingSection(long minRollingTimestamp, long maxRollingTimestamp);

  @Query("DELETE FROM cwa_token WHERE timestamp < Date(:date)")
  void purgeOldTokens(Date date);
}
