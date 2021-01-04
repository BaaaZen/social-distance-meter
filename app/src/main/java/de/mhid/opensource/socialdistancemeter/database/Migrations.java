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

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migrations {
    static final androidx.room.migration.Migration MIGRATION_1_2 = new androidx.room.migration.Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_cwa_diag_key_rolling_start_interval_number` ON `cwa_diag_key` (`rolling_start_interval_number`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_cwa_diag_key_rolling_period` ON `cwa_diag_key` (`rolling_period`)");
        }
    };
}
