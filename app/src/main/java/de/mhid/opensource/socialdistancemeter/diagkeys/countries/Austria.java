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
package de.mhid.opensource.socialdistancemeter.diagkeys.countries;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.diagkeys.Downloader;
import de.mhid.opensource.socialdistancemeter.diagkeys.country.CountryWithDailyKeys;
import de.mhid.opensource.socialdistancemeter.diagkeys.parser.TemporaryExposureKeyExportParser;
import de.mhid.opensource.socialdistancemeter.utils.Timestamp;

public class Austria extends CountryWithDailyKeys {
    private HashMap<Long, String> cachedAvailableDates = null;

    @Override
    public void preKeyUpdate() {
        cachedAvailableDates = null;
    }

    @Override
    public void postKeyUpdate() {
        cachedAvailableDates = null;
    }

    @Override
    protected String getCountryBaseUrl() {
        return "https://cdn.prod-rca-coronaapp-fd.net";
    }

    @Override
    public int getCountrySettingStringId() {
        return R.string.settings_key_risk_sync_country_enabled_at;
    }

    @Override
    public String getCountryCode() {
        return "at";
    }

    @Override
    public int getCountryName() {
        return R.string.country_at;
    }

    public String getIndexURL() {
        return getCountryBaseUrl() + "/exposures/at/index.json";
    }

    @Override
    public List<String> getAvailableDates() {
        if(cachedAvailableDates == null) {
            try {
                Object jsonObject = Downloader.requestJson(getIndexURL());
                if(!(jsonObject instanceof JSONObject)) {
                    Log.e(getClass().getSimpleName(), "Download error: Expected JSONObject for type of batches");
                    return null;
                } else if(!((JSONObject) jsonObject).has("daily_batches")) {
                    Log.e(getClass().getSimpleName(), "Download error: JSONObject has no key \"daily_batches\"");
                    return null;
                }

                JSONArray dailyBatches = ((JSONObject) jsonObject).getJSONArray("daily_batches");

                cachedAvailableDates = new HashMap<>();
                for(int i=0; i<dailyBatches.length(); i++) {
                    JSONObject json = dailyBatches.getJSONObject(i);
                    long interval = json.getLong("interval");
                    JSONArray batchFiles = json.getJSONArray("batch_file_paths");

                    if(batchFiles.length() < 1) continue;
                    if(batchFiles.length() > 1) {
                        Log.w(getClass().getSimpleName(), "Day has multiple batch files for interval " + interval);
                    }

                    String batchUri = batchFiles.getString(0);

                    cachedAvailableDates.put(interval, batchUri);
                }
            } catch (Downloader.DownloadException e) {
                Log.e(getClass().getSimpleName(), "Download error", e);
                return null;
            } catch (JSONException e) {
                Log.e(getClass().getSimpleName(), "Download error: Invalid JSON response", e);
                return null;
            }
        }

        ArrayList<String> availableDates = new ArrayList<>();
        for(Long key : cachedAvailableDates.keySet()) {
            availableDates.add(Long.toString(key));
        }

        return availableDates;
    }

    @Override
    public boolean isDailyKeyValid(String date) {
        long refTimestamp = Timestamp.getCurrentTimestamp().getRollingTimestamp() - 144*15;
        return Long.parseLong(date) > refTimestamp;
    }

    @Override
    public TemporaryExposureKeyExportParser.TemporaryExposureKeyExport getParsedKeysForDate(String date) throws CountryDownloadException {
        if(cachedAvailableDates == null) throw new CountryDownloadException("Missing index information");

        Long interval = Long.parseLong(date);
        if(!cachedAvailableDates.containsKey(interval)) throw new CountryDownloadException("Date not found in index");

        String url = getCountryBaseUrl() + cachedAvailableDates.get(interval);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Downloader.requestDownload(url, baos);
            return parseKeysFromDownload(baos.toByteArray());
        } catch (Downloader.DownloadException e) {
            // error downloading file
            throw new CountryDownloadException("Download error", e);
        }
    }
}
