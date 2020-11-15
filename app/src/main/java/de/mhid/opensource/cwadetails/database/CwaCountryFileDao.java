package de.mhid.opensource.cwadetails.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CwaCountryFileDao {
    @Insert
    long insert(CwaCountryFile countryFile);

    @Query("SELECT * FROM cwa_country_file WHERE country_id = :countryId")
    List<CwaCountryFile> getAllForCountry(long countryId);

    @Query("DELETE FROM cwa_country_file WHERE country_id = :countryId AND filename = :filename")
    void deleteForCountry(long countryId, String filename);
}
