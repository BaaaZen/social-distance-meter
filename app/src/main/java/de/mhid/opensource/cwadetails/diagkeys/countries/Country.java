package de.mhid.opensource.cwadetails.diagkeys.countries;

import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.mhid.opensource.cwadetails.diagkeys.Downloader;
import de.mhid.opensource.cwadetails.diagkeys.parser.TemporaryExposureKeyExportParser;

public abstract class Country {
    public final static Country[] countries = {
            new Germany()
    };

    public class CountryDownloadException extends Exception {
        public CountryDownloadException(String s) { super(s); }
        public CountryDownloadException(String s, Throwable e) { super(s, e); }
    }

    protected Country() {}

    protected abstract String getCountryBaseUrl();
    public abstract String getCountryCode();
    public abstract int getCountryName();

    private String getDateUrl() {
        return getCountryBaseUrl() + "/date";
    }

    private String getDateUrl(String date) {
        return getCountryBaseUrl() + "/date/" + date;
    }

    public String[] getAvailableDates() {
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

            return dates.toArray(new String[dates.size()]);
        } catch (Downloader.DownloadException e) {
            Log.e(getClass().getSimpleName(), "Download error", e);
            return null;
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Download error: Invalid JSON response", e);
            return null;
        }
    }

    public TemporaryExposureKeyExportParser.TemporaryExposureKeyExport getParsedKeysForDate(String date) throws CountryDownloadException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Downloader.requestDownload(getDateUrl(date), baos);
        } catch (Downloader.DownloadException e) {
            // error downloading file
            throw new CountryDownloadException("Download error", e);
        }

        HashMap<String, ByteArrayOutputStream> fileContent = new HashMap<>();
        try {
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()));
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
            ByteArrayInputStream bais = new ByteArrayInputStream(fileContent.get("export.bin").toByteArray());

            // check header
            byte[] header = new byte[16];
            bais.read(header);
            if(!Arrays.equals(header, "EK Export v1    ".getBytes())) {
                throw new CountryDownloadException("File export.bin has invalid header");
            }

            return TemporaryExposureKeyExportParser.TemporaryExposureKeyExport.parseFrom(bais);
        } catch (InvalidProtocolBufferException e) {
            // invalid export.bin file
            throw new CountryDownloadException("Error while parsing export.bin file", e);
        } catch (IOException e) {
            // invalid export.bin file
            throw new CountryDownloadException("Error while parsing export.bin file", e);
        }
    }
}
