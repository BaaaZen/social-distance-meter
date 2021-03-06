/*
Social Distance Meter - An app to analyze and rate your social distancing behavior
Copyright (C) 2020-2021  Mirko Hansen (baaazen@gmail.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package de.mhid.opensource.socialdistancemeter.database;

import android.content.Context;
import android.os.AsyncTask;

import androidx.room.Room;

public class Database {
  public interface RunnableWithReturn<T> {
    T run();
  }

  public static Database getInstance(Context ctx) {
    return new Database(ctx);
  }


  private final Context ctx;
  private Database(Context ctx) {
    this.ctx = ctx;
  }

  public CwaDatabase cwaDatabase() {
    return Room.databaseBuilder(ctx, CwaDatabase.class, "cwa-token.db")
            .addMigrations(Migrations.MIGRATION_1_2)
            .build();
  }

  public void runAsync(Runnable runnable) {
    Runnable r = () -> {
      synchronized (Database.this) {
        runnable.run();
      }
    };

    AsyncTask.execute(r);
  }

  public <T> T runSync(RunnableWithReturn<T> runnable) {
    synchronized (Database.this) {
      return runnable.run();
    }
  }

}