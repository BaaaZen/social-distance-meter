/*
Social Distance Meter - An app to analyze and rate your social distancing behavior
Copyright (C) 2020-2021  Mirko Hansen (baaazen@gmail.com)

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

import java.util.List;

@Dao
public interface CwaDiagKeyDao {
    @Insert
    long[] insert(List<CwaDiagKey> diagKeys);

    @Update
    Void update(List<CwaDiagKey> diagKeys);

    @Query("SELECT COUNT(*) as count FROM cwa_diag_key WHERE (rolling_start_interval_number >= :minRollingTimestamp OR rolling_start_interval_number + rolling_period > :minRollingTimestamp) AND flag_checked = 0")
    CwaDiagKeyCount getCountUncheckedInRollingSection(long minRollingTimestamp);

    @Query("SELECT * FROM cwa_diag_key WHERE (rolling_start_interval_number >= :minRollingTimestamp OR rolling_start_interval_number + rolling_period > :minRollingTimestamp) AND flag_checked = 0 ORDER BY rolling_start_interval_number ASC, rolling_period ASC LIMIT :limit")
    List<CwaDiagKey> getUncheckedInRollingSection(long minRollingTimestamp, int limit);

    @Query("SELECT COUNT(*) as count FROM cwa_diag_key")
    CwaDiagKeyCount getCount();
}
