package de.mhid.opensource.cwadetails.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {CwaToken.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class CwaDatabase extends RoomDatabase {
  public abstract CwaTokenDao cwaTokenDao();
}
