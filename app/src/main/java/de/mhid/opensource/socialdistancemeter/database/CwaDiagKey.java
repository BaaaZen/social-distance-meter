/*
Social Distance Meter - An app to analyze and rate your social distancing behavior
Copyright (C) 2020  Mirko Hansen (baaazen@gmail.com)

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
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "cwa_diag_key",
        foreignKeys = @ForeignKey(entity = CwaCountryFile.class, parentColumns = "id", childColumns = "file_id", onDelete = ForeignKey.CASCADE),
        indices = { @Index("file_id") }
)
public class CwaDiagKey {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "file_id")
    public long fileId;

    @NonNull
    @ColumnInfo(name = "flag_checked")
    public boolean flagChecked = false;

    @NonNull
    @ColumnInfo(name = "key_data")
    public String keyData;

    @NonNull
    @ColumnInfo(name = "transmission_risk_level")
    public int transmissionRiskLevel;

    @NonNull
    @ColumnInfo(name = "rolling_start_interval_number")
    public long rollingStartIntervalNumber;

    @NonNull
    @ColumnInfo(name = "rolling_period")
    public int rollingPeriod;

    @NonNull
    @ColumnInfo(name = "report_type")
    public int reportType;

    @NonNull
    @ColumnInfo(name = "days_since_onset_of_symptoms")
    public int daysSinceOnsetOfSymptoms;
}
