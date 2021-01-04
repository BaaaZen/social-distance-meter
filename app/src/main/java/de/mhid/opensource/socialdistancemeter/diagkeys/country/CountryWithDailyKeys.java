/*
Social Distance Meter - An app to analyze and rate your social distancing behavior
Copyright (C) 2021  Mirko Hansen (baaazen@gmail.com)

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
package de.mhid.opensource.socialdistancemeter.diagkeys.country;

import java.util.List;

import de.mhid.opensource.socialdistancemeter.diagkeys.parser.TemporaryExposureKeyExportParser;

public abstract class CountryWithDailyKeys extends Country {
    public abstract List<String> getAvailableDates();
    public abstract boolean isDailyKeyValid(String date);
    public abstract TemporaryExposureKeyExportParser.TemporaryExposureKeyExport getParsedKeysForDate(String date) throws CountryDownloadException;
}
