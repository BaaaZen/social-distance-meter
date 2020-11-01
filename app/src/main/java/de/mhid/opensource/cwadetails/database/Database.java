package de.mhid.opensource.cwadetails.database;

import android.content.Context;

import androidx.room.Room;

public class Database {
  private static Database instance = null;
  public static Database getInstance(Context ctx) {
    if(instance == null) instance = new Database(ctx);
    return instance;
  }


  private CwaDatabase cwaDatabase;
  private Database(Context ctx) {
    cwaDatabase = Room.databaseBuilder(ctx, CwaDatabase.class, "cwa-token.db").build();
  }

}
