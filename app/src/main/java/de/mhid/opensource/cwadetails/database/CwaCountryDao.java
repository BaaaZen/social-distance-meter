package de.mhid.opensource.cwadetails.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CwaCountryDao {
    @Insert
    void insert(CwaCountry country);

    @Query("SELECT * FROM cwa_country")
    List<CwaCountry> getAll();

    @Query("DELETE FROM cwa_country WHERE country_code = :countryCode")
    void delete(String countryCode);
}
