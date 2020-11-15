package de.mhid.opensource.cwadetails.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CwaCountryDao {
    @Insert
    long insert(CwaCountry country);

    @Query("SELECT * FROM cwa_country")
    List<CwaCountry> getAll();

    @Query("SELECT * FROM cwa_country WHERE country_code = :countryCode")
    CwaCountry getCountryByCountryCode(String countryCode);

    @Query("DELETE FROM cwa_country WHERE country_code = :countryCode")
    void delete(String countryCode);
}
