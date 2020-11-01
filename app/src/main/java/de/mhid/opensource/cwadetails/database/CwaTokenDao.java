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

  @Query("SELECT * FROM cwa_token WHERE timestamp = Date(:date)")
  List<CwaToken> getFromDate(Date date);
}
