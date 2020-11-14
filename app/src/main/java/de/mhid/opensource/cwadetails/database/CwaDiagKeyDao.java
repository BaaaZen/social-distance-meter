package de.mhid.opensource.cwadetails.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CwaDiagKeyDao {
    @Insert
    void insert(CwaDiagKey diagKey);

    @Query("SELECT * FROM cwa_diag_key WHERE flag_checked = 0 ORDER BY rolling_start_interval_number ASC, rolling_period ASC")
    List<CwaDiagKey> getAllUnchecked();
}
