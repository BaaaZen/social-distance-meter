package de.mhid.opensource.socialdistancemeter.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "cwa_country",
        indices = { @Index(value = "country_code", unique = true) }
)
public class CwaCountry {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "country_code")
    public String countryCode;
}
