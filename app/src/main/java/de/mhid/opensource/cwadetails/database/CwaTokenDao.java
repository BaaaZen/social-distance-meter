package de.mhid.opensource.cwadetails.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

@Dao
public interface CwaTokenDao {
  @Insert
  void insert(CwaToken token);

  @Query("SELECT * FROM cwa_token")
  List<CwaToken> getAll();

  @Query("SELECT * FROM cwa_token WHERE diagkey_id = NULL AND rolling_timestamp >= :minRollingTimestamp AND rolling_timestamp <= :maxRollingTimestamp ORDER BY rolling_timestamp ASC, mac ASC, token ASC")
  List<CwaToken> getRollingSection(long minRollingTimestamp, long maxRollingTimestamp);

  @Query("DELETE FROM cwa_token WHERE timestamp < Date(:date)")
  void purgeOldTokens(Date date);
}
