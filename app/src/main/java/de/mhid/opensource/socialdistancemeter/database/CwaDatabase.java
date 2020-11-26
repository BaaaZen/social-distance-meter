package de.mhid.opensource.socialdistancemeter.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(
    entities = {
        CwaCountry.class,
        CwaCountryFile.class,
        CwaDiagKey.class,
        CwaToken.class
    },
    version = 1
)
@TypeConverters({Converters.class})
public abstract class CwaDatabase extends RoomDatabase {
  public abstract CwaCountryDao cwaCountry();
  public abstract CwaCountryFileDao cwaCountryFile();
  public abstract CwaDiagKeyDao cwaDiagKey();
  public abstract CwaTokenDao cwaToken();
}
