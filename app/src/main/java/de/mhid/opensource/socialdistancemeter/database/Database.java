package de.mhid.opensource.socialdistancemeter.database;

import android.content.Context;
import android.os.AsyncTask;

import androidx.room.Room;

public class Database {
  public interface RunnableWithReturn<T> {
    T run();
  }

  private static Database instance = null;
  public static Database getInstance(Context ctx) {
    if(instance == null) instance = new Database(ctx);
    return instance;
  }


//  private CwaDatabase cwaDatabase;
  private Context ctx;
  private Database(Context ctx) {
    this.ctx = ctx;
//    cwaDatabase = Room.databaseBuilder(ctx, CwaDatabase.class, "cwa-token.db").build();
  }

  public CwaDatabase cwaDatabase() {
    return Room.databaseBuilder(ctx, CwaDatabase.class, "cwa-token.db").build();
//    return cwaDatabase;
  }

  public void runAsync(Runnable runnable) {
    Runnable r = () -> {
      synchronized (instance) {
        runnable.run();
      }
    };

    AsyncTask.execute(r);
  }

  public <T> T runSync(RunnableWithReturn<T> runnable) {
    synchronized (instance) {
      return runnable.run();
    }
  }

}
