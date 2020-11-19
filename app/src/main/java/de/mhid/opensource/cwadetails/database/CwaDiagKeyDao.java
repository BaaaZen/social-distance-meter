package de.mhid.opensource.cwadetails.database;

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

    @Query("SELECT * FROM cwa_diag_key WHERE (rolling_start_interval_number >= :minRollingTimestamp OR rolling_start_interval_number + rolling_period > :minRollingTimestamp) AND flag_checked = 0 ORDER BY rolling_start_interval_number ASC, rolling_period ASC")
    List<CwaDiagKey> getUncheckedInRollingSection(long minRollingTimestamp);

    @Query("SELECT COUNT(*) as count FROM cwa_diag_key")
    CwaDiagKeyCount getCount();
}
