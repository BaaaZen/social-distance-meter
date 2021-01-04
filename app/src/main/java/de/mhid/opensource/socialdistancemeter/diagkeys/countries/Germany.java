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
package de.mhid.opensource.socialdistancemeter.diagkeys.countries;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.diagkeys.Downloader;
import de.mhid.opensource.socialdistancemeter.diagkeys.country.CountryWithHourlyKeys;
import de.mhid.opensource.socialdistancemeter.diagkeys.parser.TemporaryExposureKeyExportParser;

public class Germany extends CountryWithHourlyKeys {
    private List<String> cachedAvailableDates = null;
    private HashMap<String, List<Integer>> cachedAvailableHours = null;

    @Override
    public void preKeyUpdate() {
        cachedAvailableHours = new HashMap<>();
    }

    @Override
    public void postKeyUpdate() {
        cachedAvailableDates = null;
        cachedAvailableHours = null;
    }

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

    private String getDateUrl() { return getCountryBaseUrl() + "/date"; }
    private String getDateUrl(String date) { return getCountryBaseUrl() + "/date/" + date; }
    private String getDateHourUrl(String date) { return getCountryBaseUrl() + "/date/" + date + "/hour"; }
    private String getDateHourUrl(String date, Integer hour) { return getCountryBaseUrl() + "/date/" + date + "/hour/" + hour.toString(); }

    @Override
    public List<String> getAvailableDates() {
        if(cachedAvailableDates == null) {
            try {
                Object jsonObject = Downloader.requestJson(getDateUrl());
                if(!(jsonObject instanceof JSONArray)) {
                    Log.e(getClass().getSimpleName(), "Download error: Expected JSONArray for list of dates");
                    return null;
                }

                ArrayList<String> dates = new ArrayList<>();
                JSONArray json = (JSONArray)jsonObject;
                for(int i=0; i<json.length(); i++) {
                    dates.add(json.getString(i));
                }

                cachedAvailableDates = dates;
            } catch (Downloader.DownloadException e) {
                Log.e(getClass().getSimpleName(), "Download error", e);
                return null;
            } catch (JSONException e) {
                Log.e(getClass().getSimpleName(), "Download error: Invalid JSON response", e);
                return null;
            }
        }

        return cachedAvailableDates;
    }

    @Override
    public boolean isDailyKeyValid(String date) {
        List<String> availableDates = getAvailableDates();
        return availableDates.contains(date);
    }

    private void downloadKeysForDate(OutputStream os, String date) throws CountryDownloadException {
        try {
            Downloader.requestDownload(getDateUrl(date), os);
        } catch (Downloader.DownloadException e) {
            // error downloading file
            throw new CountryDownloadException("Download error", e);
        }
    }

    @Override
    public TemporaryExposureKeyExportParser.TemporaryExposureKeyExport getParsedKeysForDate(String date) throws CountryDownloadException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        downloadKeysForDate(baos, date);
        return parseKeysFromDownload(baos.toByteArray());
    }

    @Override
    public List<Integer> getAvailableHours(String date) {
        if(!cachedAvailableHours.containsKey(date)) {
            try {
                Object jsonObject = Downloader.requestJson(getDateHourUrl(date));
                if(!(jsonObject instanceof JSONArray)) {
                    Log.e(getClass().getSimpleName(), "Download error: Expected JSONArray for list of dates");
                    return null;
                }

                ArrayList<Integer> hours = new ArrayList<>();
                JSONArray json = (JSONArray)jsonObject;
                for(int i=0; i<json.length(); i++) {
                    hours.add(json.getInt(i));
                }

                cachedAvailableHours.put(date, hours);
            } catch (Downloader.DownloadException e) {
                Log.e(getClass().getSimpleName(), "Download error", e);
                return null;
            } catch (JSONException e) {
                Log.e(getClass().getSimpleName(), "Download error: Invalid JSON response", e);
                return null;
            }
        }

        return cachedAvailableHours.get(date);
    }

    private void downloadKeysForDateHour(OutputStream os, String date, Integer hour) throws CountryDownloadException {
        try {
            Downloader.requestDownload(getDateHourUrl(date, hour), os);
        } catch (Downloader.DownloadException e) {
            // error downloading file
            throw new CountryDownloadException("Download error", e);
        }
    }

    @Override
    public TemporaryExposureKeyExportParser.TemporaryExposureKeyExport getParsedKeysForDateHour(String date, Integer hour) throws CountryDownloadException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        downloadKeysForDateHour(baos, date, hour);
        return parseKeysFromDownload(baos.toByteArray());
    }
}
