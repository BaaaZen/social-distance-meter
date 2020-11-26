package de.mhid.opensource.socialdistancemeter.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "cwa_country_file",
        foreignKeys = @ForeignKey(entity = CwaCountry.class, parentColumns = "id", childColumns = "country_id", onDelete = ForeignKey.CASCADE),
        indices = { @Index("country_id"), @Index(value = { "country_id", "filename" }, unique = true) }
)
public class CwaCountryFile {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "country_id")
    public long countryId;

    @NonNull
    @ColumnInfo(name = "filename")
    public String filename;

    @NonNull
    @ColumnInfo(name = "timestamp")
    public Date timestamp;
}
