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

  @Query("UPDATE cwa_token SET diagkey_id = :diagKey WHERE token = :token AND mac = :mac AND rolling_timestamp = :rollingTimestamp")
  void linkTokenToDiagKey(long diagKey, String token, String mac, long rollingTimestamp);

  @Query("SELECT * FROM cwa_token")
  List<CwaToken> getAll();

  @Query("SELECT MIN(rolling_timestamp) as minRollingTimestamp FROM cwa_token")
  CwaTokenMinRollingTimestamp getMinRollingTimestamp();

  @Query("SELECT mac, rssi, local_timestamp/600000 as localRollingTimestamp FROM cwa_token WHERE local_timestamp/600000 >= :minRollingTimestamp AND local_timestamp/600000 <= :maxRollingTimestamp ORDER BY rolling_timestamp DESC, rssi ASC, mac ASC")
  List<CwaTokenStatistics> getStatistics(long minRollingTimestamp, long maxRollingTimestamp);

  @Query("SELECT DISTINCT token, mac, rolling_timestamp as rollingTimestamp FROM cwa_token WHERE diagkey_id IS NULL AND rolling_timestamp >= :minRollingTimestamp AND rolling_timestamp < :maxRollingTimestamp ORDER BY token ASC")
  List<CwaTokenDistinct> getDistinctTokensForDay(long minRollingTimestamp, long maxRollingTimestamp);

  @Query("SELECT diagkey_id as diagKeyId, MIN(local_timestamp) as minLocalTimestamp, MAX(local_timestamp) as maxLocalTimestamp FROM cwa_token WHERE diagkey_id = null GROUP BY diagkey_id;")
  List<CwaTokenRisk> getRisks();

  @Query("DELETE FROM cwa_token WHERE rolling_timestamp < :rollingTimestamp")
  void purgeOldTokens(long rollingTimestamp);
}
