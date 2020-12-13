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
package de.mhid.opensource.socialdistancemeter.diagkeys;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.activity.maincards.CardRisks;
import de.mhid.opensource.socialdistancemeter.database.CwaCountry;
import de.mhid.opensource.socialdistancemeter.database.CwaCountryFile;
import de.mhid.opensource.socialdistancemeter.database.CwaDiagKey;
import de.mhid.opensource.socialdistancemeter.database.CwaToken;
import de.mhid.opensource.socialdistancemeter.database.CwaTokenDistinct;
import de.mhid.opensource.socialdistancemeter.database.CwaTokenMinRollingTimestamp;
import de.mhid.opensource.socialdistancemeter.database.Database;
import de.mhid.opensource.socialdistancemeter.diagkeys.countries.Country;
import de.mhid.opensource.socialdistancemeter.diagkeys.countries.CountryList;
import de.mhid.opensource.socialdistancemeter.diagkeys.parser.TemporaryExposureKeyExportParser;
import de.mhid.opensource.socialdistancemeter.notification.NotificationChannelHelper;
import de.mhid.opensource.socialdistancemeter.services.DiagKeySyncService;
import de.mhid.opensource.socialdistancemeter.utils.HexString;

public class DiagKeySyncWorker extends Worker {
    public final static String WORK_PARAMETER_BACKGROUND = "background";

    public final static int ROLLING_TIMESTAMP_MATCH_THRESHOLD = 3; // 30 mins

    private static final ReentrantLock lock = new ReentrantLock();
    public static boolean isRunning() {
       synchronized (lock) {
           return lock.isLocked();
       }
    }
//    private static Date lastExecution = null;
    private static boolean lockConcurrency() {
        synchronized (lock) {
//            if(lastExecution != null) {
//                if(TimeUnit.MINUTES.convert(new Date().getTime() - lastExecution.getTime(), TimeUnit.MILLISECONDS) < 10) {
//                    // wait at least 10 minutes before re-syncing
//                    return false;
//                }
//            }

            if(lock.isLocked()) return false;
            lock.lock();
            return true;
        }
    }

    private static void unlockConcurrency() {
        synchronized (lock) {
            lock.unlock();

//            lastExecution = new Date();
        }
    }

    static class TimeslotDiagKeys {
        private long rollingStartIntervalNumber;
        private long rollingEndIntervalNumber;
        private final List<DiagKeyCrypto> diagKeys = new ArrayList<>();
        public TimeslotDiagKeys(CwaDiagKey referenceDiagKey) {
            this.rollingStartIntervalNumber = referenceDiagKey.rollingStartIntervalNumber;
            this.rollingEndIntervalNumber = referenceDiagKey.rollingStartIntervalNumber + referenceDiagKey.rollingPeriod;
        }

        public boolean belongsToThisTimeslot(CwaDiagKey diagKey) {
            return diagKey.rollingStartIntervalNumber == rollingStartIntervalNumber
                || diagKey.rollingStartIntervalNumber + diagKey.rollingPeriod == rollingEndIntervalNumber
                || (
                    diagKey.rollingStartIntervalNumber <= rollingStartIntervalNumber
                    && diagKey.rollingStartIntervalNumber + diagKey.rollingPeriod >= rollingEndIntervalNumber
                );
        }

        public void addDiagKey(CwaDiagKey diagKey) {
            if(diagKey.rollingStartIntervalNumber < rollingStartIntervalNumber) rollingStartIntervalNumber = diagKey.rollingStartIntervalNumber;
            if(diagKey.rollingStartIntervalNumber + diagKey.rollingPeriod < rollingEndIntervalNumber) rollingEndIntervalNumber = diagKey.rollingStartIntervalNumber + diagKey.rollingPeriod;
            diagKeys.add(new DiagKeyCrypto(diagKey));
        }

        public List<Pair<String, DiagKeyCrypto>> getSortedRPIsForDiagKeys() throws DiagKeyCrypto.CryptoError {
            ArrayList<Pair<String,DiagKeyCrypto>> sortedRPIs = new ArrayList<>();
            for (DiagKeyCrypto diagKey : diagKeys) {
                sortedRPIs.addAll(diagKey.getAllRPIs());
            }
            Collections.sort(sortedRPIs, (o1, o2) -> o1.first.compareTo(o2.first));
            return sortedRPIs;
        }

        public List<CwaDiagKey> getDiagKeys() {
            ArrayList<CwaDiagKey> diagKeys = new ArrayList<>();
            for(DiagKeyCrypto diagKey : this.diagKeys) {
                diagKeys.add(diagKey.getDiagKey());
            }
            return diagKeys;
        }

        public String getDateString() {
            Date d = new Date(rollingStartIntervalNumber * 10 * 60 * 1000);
            return DateFormat.getDateInstance(DateFormat.MEDIUM).format(d);
        }
    }

//    static class TimeslotTokens {
//        private final CwaToken referenceToken;
//        private final List<CwaToken> tokens = new ArrayList<>();
//        public TimeslotTokens(CwaToken referenceToken) {
//            this.referenceToken = referenceToken;
//        }
//
//        public boolean belongsToThisTimeslot(CwaToken token) {
//            return token.rollingTimestamp == referenceToken.rollingTimestamp;
//        }
//
//        public void addToken(CwaToken token) {
//            tokens.add(token);
//        }
//
//        public List<CwaToken> getTokens() {
//            return tokens;
//        }
//    }


    private final SharedPreferences sharedPreferences;

    public DiagKeySyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull String description, int progress) {
        Context ctx = getApplicationContext();

        // build notification
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx, NotificationChannelHelper.getChannelId(ctx))
                        .setContentTitle(ctx.getString(R.string.notification_sync_title))
                        .setContentText(description)
                        .setSmallIcon(R.drawable.round_sync_24)
                        .setOngoing(true)
                        .setProgress(100, progress, progress == 0);

        return new ForegroundInfo(2, builder.build());
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean isSettingsSyncEnabled = sharedPreferences.getBoolean(getApplicationContext().getString(R.string.settings_key_risk_sync_enabled), true);
        boolean isBackgroundSync = getInputData().getBoolean(WORK_PARAMETER_BACKGROUND, true);
        if(isBackgroundSync && !isSettingsSyncEnabled) {
            // if sync is disabled in settings -> skip background work
            return Result.success();
        }

        setForegroundAsync(createForegroundInfo(getApplicationContext().getString(R.string.card_risks_sync_status_starting_download), 0));

        Log.i(getClass().getSimpleName(), "Starting to work ... :-)");
        String error = getApplicationContext().getString(R.string.card_risks_sync_status_error_unknown);

        boolean lockSuccess = lockConcurrency();
        if(!lockSuccess) {
            // another worker is already running -> abort current work
            Log.i(getClass().getSimpleName(), "Another worker is already in progress, so aborting work for now!");
            return Result.success();
        }

        try {
            sendSyncStatusUpdate(getApplicationContext().getString(R.string.card_risks_sync_status_starting_download),0);

            List<Country> countries = new ArrayList<>(Arrays.asList(CountryList.COUNTRIES));

            boolean success = downloadDailyKeys(countries);
            if(!success) error = getApplicationContext().getString(R.string.card_risks_sync_status_error_download);

            sendSyncStatusUpdate(getApplicationContext().getString(R.string.card_risks_sync_status_starting_compare),50);
            Log.i(getClass().getSimpleName(), "Downloading is done with success: " + success);

            boolean checkSuccess = checkDiagKeys();
            if(!checkSuccess) error = getApplicationContext().getString(R.string.card_risks_sync_status_error_checking);
            success &= checkSuccess;

            Log.i(getClass().getSimpleName(), "Work is done with success: " + success);
            if (success) {
                Date d = new Date();
                String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(d);
                String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(d);
                String dateTime = getApplicationContext().getString(R.string.date_time_format, date, time);

                sharedPreferences
                        .edit()
                        .putString(getApplicationContext().getString(R.string.internal_settings_key_last_scan_timestamp), dateTime)
                        .apply();

                // trigger update of risk dialog via intent
                sendIntentGetUpdates();

                // trigger hide sync status
                sendIntentSyncStatusUpdateDone();

                // trigger update encounters
                sendIntentEncountersUpdate();
            } else {
                // show sync error
                sendIntentSyncStatusUpdateError(error);
            }
            return Result.success();
        } finally {
            unlockConcurrency();
        }
    }

    private void sendIntentGetUpdates() {
        Intent sndGetUpdates = new Intent(getApplicationContext(), DiagKeySyncService.class);
        sndGetUpdates.setAction(DiagKeySyncService.INTENT_GET_UPDATES);
        getApplicationContext().startService(sndGetUpdates);
    }

    private void sendIntentSyncStatusUpdateDone() {
        Intent sndSyncStatusUpdate = new Intent();
        sndSyncStatusUpdate.setAction(CardRisks.INTENT_SYNC_STATUS_SYNC);
        sndSyncStatusUpdate.putExtra(CardRisks.INTENT_SYNC_STATUS_SYNC__RUNNING, false);
        getApplicationContext().sendBroadcast(sndSyncStatusUpdate);
    }

    private void sendIntentSyncStatusUpdateError(String error) {
        Intent sndSyncStatusUpdate = new Intent();
        sndSyncStatusUpdate.setAction(CardRisks.INTENT_SYNC_STATUS_SYNC);
        sndSyncStatusUpdate.putExtra(CardRisks.INTENT_SYNC_STATUS_SYNC__ERROR, true);
        sndSyncStatusUpdate.putExtra(CardRisks.INTENT_SYNC_STATUS_SYNC__DESCRIPTION, error);
        getApplicationContext().sendBroadcast(sndSyncStatusUpdate);
    }

    private void sendSyncStatusUpdate(String description, int progress) {
        setForegroundAsync(createForegroundInfo(description, progress));
        sendIntentSyncStatusUpdate(description, progress);
    }

    private void sendIntentSyncStatusUpdate(String description, int progress) {
        Intent sndSyncStatusUpdate = new Intent();
        sndSyncStatusUpdate.setAction(CardRisks.INTENT_SYNC_STATUS_SYNC);
        sndSyncStatusUpdate.putExtra(CardRisks.INTENT_SYNC_STATUS_SYNC__RUNNING, true);
        sndSyncStatusUpdate.putExtra(CardRisks.INTENT_SYNC_STATUS_SYNC__DESCRIPTION, description);
        sndSyncStatusUpdate.putExtra(CardRisks.INTENT_SYNC_STATUS_SYNC__PROGRESS, progress);
        getApplicationContext().sendBroadcast(sndSyncStatusUpdate);
    }

    private void sendIntentEncountersUpdate() {
        Intent sndEncountersUpdate = new Intent();
        sndEncountersUpdate.setAction(CardRisks.INTENT_ENCOUNTERS_UPDATE);
        getApplicationContext().sendBroadcast(sndEncountersUpdate);
    }

    private boolean downloadDailyKeys(List<Country> countries) {
        boolean success = true;
        HashSet<String> availableCountries = new HashSet<>();
        int countryCounter = 0;
        for(Country country : countries) {
            // status update
            String countryName = getApplicationContext().getString(country.getCountryName());
            int progress = 10 + countryCounter * 40 / countries.size();
            sendSyncStatusUpdate(getApplicationContext().getString(R.string.card_risks_sync_status_download_country, countryName), progress);
            countryCounter++;

            // downloading keys
            success &= downloadDailyKeysForCountry(country);
            availableCountries.add(country.getCountryCode());
        }

        Database db = Database.getInstance(getApplicationContext());
        List<CwaCountry> cwaCountries = db.runSync(() -> db.cwaDatabase().cwaCountry().getAll());
        for(CwaCountry cwaCountry : cwaCountries) {
            if(!availableCountries.contains(cwaCountry.countryCode)) {
                // delete country from db
                db.runSync((Database.RunnableWithReturn<Void>) () -> {
                    db.cwaDatabase().cwaCountry().delete(cwaCountry.countryCode);
                    return null;
                });
            }
        }

        return success;
    }

    private boolean downloadDailyKeysForCountry(Country country) {
        // find country in DB
        Database db = Database.getInstance(getApplicationContext());

        final long cwaCountryId;
        CwaCountry cwaCountry = db.runSync(() -> db.cwaDatabase().cwaCountry().getCountryByCountryCode(country.getCountryCode()));
        if(cwaCountry == null) {
            // country not yet in DB, so let's create one
            final CwaCountry insertCwaCountry = new CwaCountry();
            insertCwaCountry.countryCode = country.getCountryCode();
            cwaCountryId = db.runSync(() -> db.cwaDatabase().cwaCountry().insert(insertCwaCountry));
        } else {
            cwaCountryId = cwaCountry.id;
        }

        // now fetch all available days
        List<String> availableDates = country.getAvailableDates();
        if(availableDates == null) {
            // error fetching dates!
            return false;
        }

        // list all files in database
        HashSet<String> dbCompleteDates = new HashSet<>();
        HashMap<String, List<String>> dbAvailableFilesPerDate = new HashMap<>();
        List<CwaCountryFile> cwaCountryFiles = db.runSync(() -> db.cwaDatabase().cwaCountryFile().getAllForCountry(cwaCountryId));
        for(CwaCountryFile cwaCountryFile : cwaCountryFiles) {
            String filename = cwaCountryFile.filename;
            if(filename.contains("/")) {
                // date/hour key file
                String date = filename.split("/", 1)[0];
                if(!dbAvailableFilesPerDate.containsKey(date)) dbAvailableFilesPerDate.put(date, new ArrayList<>());
                //noinspection ConstantConditions
                dbAvailableFilesPerDate.get(date).add(filename);
                //noinspection ConstantConditions
                if(dbAvailableFilesPerDate.get(date).size() == 24) dbCompleteDates.add(date);
            } else {
                // only date key file
                if(!dbAvailableFilesPerDate.containsKey(filename)) dbAvailableFilesPerDate.put(filename, new ArrayList<>());
                //noinspection ConstantConditions
                dbAvailableFilesPerDate.get(filename).add(filename);
                dbCompleteDates.add(filename);
            }
        }

        // fetch new days
        boolean success = true;
        for(String availableDate : availableDates) {
            if(!dbAvailableFilesPerDate.containsKey(availableDate) || (!dbCompleteDates.contains(availableDate) && !country.offersHourlyKeys())) {
                // no partial/hourly keys for this date -> download whole day
                success &= downloadDailyKeysForCountryForDay(country, cwaCountryId, availableDate);
            } else if(!dbCompleteDates.contains(availableDate) && country.offersHourlyKeys()) {
                // partial keys already in db -> check new hours
                success &= downloadDailyKeysForCountryForDayByHours(country, cwaCountryId, availableDate, dbAvailableFilesPerDate.get(availableDate));
            }
            dbAvailableFilesPerDate.remove(availableDate);
        }

        // for hourly keys -> update "today"
        if(country.offersHourlyKeys()) {
            @SuppressLint("SimpleDateFormat") String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            if(!availableDates.contains(today)) {
                // already processed date?
                success &= downloadDailyKeysForCountryForDayByHours(country, cwaCountryId, today, dbAvailableFilesPerDate.get(today));
                dbAvailableFilesPerDate.remove(today);
            }
        }

        // delete old days
        for(String removeDate : dbAvailableFilesPerDate.keySet()) {
            //noinspection ConstantConditions
            for(String filename : dbAvailableFilesPerDate.get(removeDate)) {
                db.runSync((Database.RunnableWithReturn<Void>) () -> {
                    db.cwaDatabase().cwaCountryFile().deleteForCountry(cwaCountryId, filename);
                    return null;
                });
            }
        }

        return success;
    }

    private boolean downloadDailyKeysForCountryForDay(Country country, long countryId, String day) {
        try {
            Log.d(getClass().getSimpleName(), "parsing export.bin ...");
            TemporaryExposureKeyExportParser.TemporaryExposureKeyExport tek = country.getParsedKeysForDate(day);
            Log.d(getClass().getSimpleName(), "parsing export.bin DONE");

            importTekInDatabase(countryId, day, tek);

            return true;
        } catch (Country.CountryDownloadException e) {
            Log.e(getClass().getSimpleName(), "Error downloading " + country.getCountryCode() + " -> " + day + ": " + e.getLocalizedMessage());
            return false;
        }
    }

    private boolean downloadDailyKeysForCountryForDayByHours(Country country, long countryId, String day, List<String> dbAvailableHours) {
        List<Integer> hours = country.getAvailableHours(day);
        for(Integer hour : hours) {
            String filename = day + "/" + hour.toString();
            // hourly file already downloaded?
            if(dbAvailableHours != null && dbAvailableHours.contains(filename)) continue;
            try {
                Log.d(getClass().getSimpleName(), "parsing export.bin ...");
                TemporaryExposureKeyExportParser.TemporaryExposureKeyExport tek = country.getParsedKeysForDateHour(day, hour);
                Log.d(getClass().getSimpleName(), "parsing export.bin DONE");

                importTekInDatabase(countryId, filename, tek);
            } catch (Country.CountryDownloadException e) {
                Log.e(getClass().getSimpleName(), "Error downloading " + country.getCountryCode() + " -> " + day + ": " + e.getLocalizedMessage());
                return false;
            }
        }

        return true;
    }

    private void importTekInDatabase(long countryId, String filename, TemporaryExposureKeyExportParser.TemporaryExposureKeyExport tek) {
        Database db = Database.getInstance(getApplicationContext());

        Log.d(getClass().getSimpleName(), "creating file \"" + filename + "\" in DB ...");
        // insert new file in db
        CwaCountryFile cwaCountryFile = new CwaCountryFile();
        cwaCountryFile.countryId = countryId;
        cwaCountryFile.filename = filename;
        cwaCountryFile.timestamp = new Date();

        final long cwaCountryFileId = db.runSync(() -> db.cwaDatabase().cwaCountryFile().insert(cwaCountryFile));
        Log.d(getClass().getSimpleName(), "creating file in DB ... DONE");

        Log.d(getClass().getSimpleName(), "preparing diag key entries for DB ...");
        // now insert all diag keys in db
        List<CwaDiagKey> diagKeyList = new ArrayList<>();
        for(TemporaryExposureKeyExportParser.TemporaryExposureKey key : tek.getKeysList()) {
            CwaDiagKey cwaDiagKey = new CwaDiagKey();
            cwaDiagKey.fileId = cwaCountryFileId;
            cwaDiagKey.keyData = HexString.toHexString(key.getKeyData().toByteArray());
            //noinspection deprecation
            cwaDiagKey.transmissionRiskLevel = key.getTransmissionRiskLevel();
            cwaDiagKey.rollingStartIntervalNumber = key.getRollingStartIntervalNumber();
            cwaDiagKey.rollingPeriod = key.getRollingPeriod();
            cwaDiagKey.reportType = key.getReportType().getNumber();
            cwaDiagKey.daysSinceOnsetOfSymptoms = key.getDaysSinceOnsetOfSymptoms();

            diagKeyList.add(cwaDiagKey);
        }
        Log.d(getClass().getSimpleName(), "inserting diag key entries in DB ...");
        db.runSync(() -> db.cwaDatabase().cwaDiagKey().insert(diagKeyList));
        Log.d(getClass().getSimpleName(), "inserting diag key entries in DB ... DONE");
    }

    private boolean checkDiagKeys() {
        Database db = Database.getInstance(getApplicationContext());

        CwaTokenMinRollingTimestamp cwaTokenMinRollingTimestamp = db.runSync(() -> db.cwaDatabase().cwaToken().getMinRollingTimestamp());
        if(cwaTokenMinRollingTimestamp == null) return true;

        boolean success = true;

        // group diag keys by timeslots
        List<CwaDiagKey> diagKeyList = db.runSync(() -> db.cwaDatabase().cwaDiagKey().getUncheckedInRollingSection(cwaTokenMinRollingTimestamp.minRollingTimestamp));
        TimeslotDiagKeys timeslot = null;
        int percent = 0;
        int diagKeyCounter = 0;
        for(CwaDiagKey diagKey : diagKeyList) {
            // status precalc
            percent = diagKeyCounter * 100 / diagKeyList.size();
            diagKeyCounter++;

            if(timeslot != null && !timeslot.belongsToThisTimeslot(diagKey)) {
                success &= checkDiagKeysForDay(timeslot, percent);
                timeslot = null;
            }
            if(timeslot == null) timeslot = new TimeslotDiagKeys(diagKey);
            timeslot.addDiagKey(diagKey);
        }
        if(timeslot != null) success &= checkDiagKeysForDay(timeslot, percent);

        return success;
    }

    private boolean checkDiagKeysForDay(TimeslotDiagKeys timeslotDiagKeys, int percent) {
        // status update
        int progress = 50 + percent*50/100;
        sendSyncStatusUpdate(getApplicationContext().getString(R.string.card_risks_sync_status_comparing, timeslotDiagKeys.getDateString(), percent), progress);

        Database db = Database.getInstance(getApplicationContext());

        // get sorted list of all tokens collected this day
        int queryMargin = ROLLING_TIMESTAMP_MATCH_THRESHOLD;
        List<CwaTokenDistinct> tokenList = db.runSync(() -> db.cwaDatabase().cwaToken().getDistinctTokensForDay(timeslotDiagKeys.rollingStartIntervalNumber - queryMargin, timeslotDiagKeys.rollingEndIntervalNumber + queryMargin));
        List<Pair<String, DiagKeyCrypto>> rpiList;
        try {
            rpiList = timeslotDiagKeys.getSortedRPIsForDiagKeys();
        } catch (DiagKeyCrypto.CryptoError cryptoError) {
            Log.e(getClass().getSimpleName(), "Checking diagnosis keys not possible. Crypto error: " + cryptoError);
            return false;
        }

        ArrayList<Pair<CwaTokenDistinct, Pair<String, DiagKeyCrypto>>> potentialMatches = new ArrayList<>();
        int ptr = 0;
        for(CwaTokenDistinct token : tokenList) {
            while(true) {
                if(ptr >= rpiList.size()) break;
                Pair<String, DiagKeyCrypto> rpi = rpiList.get(ptr);
                int cmp = rpi.first.compareTo(token.token);

                // rpi greater than token
                if(cmp > 0) break;
                ptr++;

                // rpi lower than token
                if(cmp < 0) continue;

                // potential match!
                potentialMatches.add(new Pair<>(token, rpi));
            }
        }

        // validate matches
        ArrayList<Pair<CwaTokenDistinct, Pair<String, DiagKeyCrypto>>> realMatches = new ArrayList<>();
        for(Pair<CwaTokenDistinct, Pair<String, DiagKeyCrypto>> match : potentialMatches) {
            try {
                long rollingTimestampOfDiagKey = match.second.second.getRollingTimestampForRpi(match.second.first);
                if(rollingTimestampOfDiagKey == -1) continue;
                if(Math.abs(rollingTimestampOfDiagKey - match.first.rollingTimestamp) < ROLLING_TIMESTAMP_MATCH_THRESHOLD) {
                    // real match!
                    realMatches.add(match);
                }
            } catch (DiagKeyCrypto.CryptoError ignored) { }
        }

        // flag diag keys as checked
        List<CwaDiagKey> diagKeys = timeslotDiagKeys.getDiagKeys();
        if(!diagKeys.isEmpty()) {
            for(CwaDiagKey diagKey : diagKeys) {
                diagKey.flagChecked = true;
            }
            db.runSync(() -> db.cwaDatabase().cwaDiagKey().update(diagKeys));
        }

        // update matches in db
        for(Pair<CwaTokenDistinct, Pair<String, DiagKeyCrypto>> match : realMatches) {
            db.runSync((Database.RunnableWithReturn<Void>) () -> {
                db.cwaDatabase().cwaToken().linkTokenToDiagKey(match.second.second.getDiagKey().id, match.first.token, match.first.mac, match.first.rollingTimestamp);
                return null;
            });
        }

        return true;
    }
}
