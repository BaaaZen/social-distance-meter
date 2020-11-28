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
package de.mhid.opensource.socialdistancemeter.diagkeys.countries;

import de.mhid.opensource.socialdistancemeter.R;

public class Germany extends Country {
    @Override
    protected String getCountryBaseUrl() {
        return "https://svc90.main.px.t-online.de/version/v1/diagnosis-keys/country/EUR";
    }

    @Override
    public String getCountryCode() {
        return "de";
    }

    @Override
    public int getCountryName() {
        return R.string.country_de;
    }
}
