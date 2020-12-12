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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.mhid.opensource.socialdistancemeter.diagkeys.Downloader;
import de.mhid.opensource.socialdistancemeter.diagkeys.parser.TemporaryExposureKeyExportParser;

public abstract class Country {
    public static class CountryDownloadException extends Exception {
        public CountryDownloadException(String s) { super(s); }
        public CountryDownloadException(String s, Throwable e) { super(s, e); }
    }

    protected Country() {}

    protected abstract String getCountryBaseUrl();
    public abstract String getCountryCode();
    public abstract int getCountryName();
    public abstract boolean offersHourlyKeys();

    private String getDateUrl() {
        return getCountryBaseUrl() + "/date";
    }
    private String getDateUrl(String date) {
        return getCountryBaseUrl() + "/date/" + date;
    }
    private String getDateHourUrl(String date) { return getCountryBaseUrl() + "/date/" + date + "/hour"; }
    private String getDateHourUrl(String date, Integer hour) { return getCountryBaseUrl() + "/date/" + date + "/hour/" + hour.toString(); }

    public List<Integer> getAvailableHours(String date) {
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

            return hours;
        } catch (Downloader.DownloadException e) {
            Log.e(getClass().getSimpleName(), "Download error", e);
            return null;
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Download error: Invalid JSON response", e);
            return null;
        }
    }

    public List<String> getAvailableDates() {
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

            return dates;
        } catch (Downloader.DownloadException e) {
            Log.e(getClass().getSimpleName(), "Download error", e);
            return null;
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Download error: Invalid JSON response", e);
            return null;
        }
    }

    private void downloadKeysForDate(OutputStream os, String date) throws CountryDownloadException {
        try {
            Downloader.requestDownload(getDateUrl(date), os);
        } catch (Downloader.DownloadException e) {
            // error downloading file
            throw new CountryDownloadException("Download error", e);
        }
    }

    private void downloadKeysForDateHour(OutputStream os, String date, Integer hour) throws CountryDownloadException {
        try {
            Downloader.requestDownload(getDateHourUrl(date, hour), os);
        } catch (Downloader.DownloadException e) {
            // error downloading file
            throw new CountryDownloadException("Download error", e);
        }
    }

    public TemporaryExposureKeyExportParser.TemporaryExposureKeyExport getParsedKeysForDate(String date) throws CountryDownloadException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        downloadKeysForDate(baos, date);
        return parseKeysFromDownload(baos.toByteArray());
    }

    public TemporaryExposureKeyExportParser.TemporaryExposureKeyExport getParsedKeysForDateHour(String date, Integer hour) throws CountryDownloadException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        downloadKeysForDateHour(baos, date, hour);
        return parseKeysFromDownload(baos.toByteArray());
    }

    private TemporaryExposureKeyExportParser.TemporaryExposureKeyExport parseKeysFromDownload(byte[] keysZipContent) throws CountryDownloadException {
        HashMap<String, ByteArrayOutputStream> fileContent = new HashMap<>();
        try {
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(keysZipContent));
            ZipEntry zipEntry;
            while((zipEntry = zis.getNextEntry()) != null) {
                if(fileContent.containsKey(zipEntry.getName()) ||
                        !(zipEntry.getName().equals("export.bin") || zipEntry.getName().equals("export.sig"))) {
                    // twin file or invalid filename
                    throw new CountryDownloadException("Invalid file name " + zipEntry.getName() + " in downloaded zip file");
                }

                ByteArrayOutputStream content = new ByteArrayOutputStream();
                int b;
                while((b = zis.read()) != -1) {
                    content.write(b);
                }
                content.close();

                fileContent.put(zipEntry.getName(), content);
            }
        } catch (IOException e) {
            // error extracting zip file
            throw new CountryDownloadException("Error extracting zip file", e);
        }

        if(!fileContent.containsKey("export.bin") || !fileContent.containsKey("export.sig")) {
            // missing file in zip file
            throw new CountryDownloadException("Incomplete zip file");
        }

        // TODO: check signature

        try {
            //noinspection ConstantConditions
            ByteArrayInputStream bais = new ByteArrayInputStream(fileContent.get("export.bin").toByteArray());

            // check header
            byte[] header = new byte[16];
            int readCount = bais.read(header);
            if(readCount == -1) {
                throw new CountryDownloadException("Error while reading export.bin file from buffer");
            }
            if(!Arrays.equals(header, "EK Export v1    ".getBytes())) {
                throw new CountryDownloadException("File export.bin has invalid header");
            }

            return TemporaryExposureKeyExportParser.TemporaryExposureKeyExport.parseFrom(bais);
        } catch (IOException e) {
            // invalid export.bin file
            throw new CountryDownloadException("Error while parsing export.bin file", e);
        }
    }
}
