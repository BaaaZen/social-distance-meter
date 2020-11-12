package de.mhid.opensource.cwadetails.diagkeys;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader {
    public static class DownloadException extends Exception {
        public DownloadException(String s) { super(s); }
        public DownloadException(String s, Throwable e) { super(s, e); }
    }

    private Downloader() {}

    private static void fetch(String url, OutputStream os) throws IOException, DownloadException {
        URL fetchUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)fetchUrl.openConnection();
        try {
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new DownloadException("Got response code " + connection.getResponseCode() + " for HTTP request on URL " + url);

            InputStream is = connection.getInputStream();
            try {
                while (true) {
                    int b = is.read();
                    if (b == -1) break;
                    os.write(b);
                }
            } finally {
                is.close();
            }
        } finally {
            connection.disconnect();
        }
    }

    public static Object requestJson(String url) throws DownloadException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            fetch(url, baos);
            baos.close();
        } catch (IOException e) {
            throw new DownloadException("Error downloading URL " + url, e);
        }

        try {

            return new JSONTokener(baos.toString()).nextValue();
        } catch (JSONException e) {
            throw new DownloadException("Error decoding JSON from URL " + url, e);
        }
    }

    public static void requestDownload(String url, OutputStream fos) throws DownloadException {
        try {
            fetch(url, fos);
            fos.close();
        } catch (IOException e) {
            throw new DownloadException("Error downloading URL " + url, e);
        }
    }
}
