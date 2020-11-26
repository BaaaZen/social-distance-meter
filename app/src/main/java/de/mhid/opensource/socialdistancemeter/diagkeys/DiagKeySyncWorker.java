package de.mhid.opensource.socialdistancemeter.diagkeys;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.database.CwaCountry;
import de.mhid.opensource.socialdistancemeter.database.CwaCountryFile;
import de.mhid.opensource.socialdistancemeter.database.CwaDiagKey;
import de.mhid.opensource.socialdistancemeter.database.CwaToken;
import de.mhid.opensource.socialdistancemeter.database.CwaTokenMinRollingTimestamp;
import de.mhid.opensource.socialdistancemeter.database.Database;
import de.mhid.opensource.socialdistancemeter.diagkeys.countries.Country;
import de.mhid.opensource.socialdistancemeter.diagkeys.parser.TemporaryExposureKeyExportParser;
import de.mhid.opensource.socialdistancemeter.services.DiagKeySyncService;
import de.mhid.opensource.socialdistancemeter.utils.HexString;

public class DiagKeySyncWorker extends Worker {
    private static final ReentrantLock lock = new ReentrantLock();
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

    class TimeslotDiagKeys {
        private CwaDiagKey referenceDiagKey;
        private List<DiagKeyCrypto> diagKeys = new ArrayList<>();
        public TimeslotDiagKeys(CwaDiagKey referenceDiagKey) {
            this.referenceDiagKey = referenceDiagKey;
        }

        public boolean belongsToThisTimeslot(CwaDiagKey diagKey) {
            return diagKey.rollingStartIntervalNumber == referenceDiagKey.rollingStartIntervalNumber && diagKey.rollingPeriod == referenceDiagKey.rollingPeriod;
        }

        public void addDiagKey(CwaDiagKey diagKey) {
            diagKeys.add(new DiagKeyCrypto(diagKey));
        }

        public List<DiagKeyCrypto> getDiagKeys() {
            return diagKeys;
        }
    }

    class TimeslotTokens {
        private CwaToken referenceToken;
        private List<CwaToken> tokens = new ArrayList<>();
        public TimeslotTokens(CwaToken referenceToken) {
            this.referenceToken = referenceToken;
        }

        public boolean belongsToThisTimeslot(CwaToken token) {
            return token.rollingTimestamp == referenceToken.rollingTimestamp;
        }

        public void addToken(CwaToken token) {
            tokens.add(token);
        }

        public List<CwaToken> getTokens() {
            return tokens;
        }
    }


    private SharedPreferences sharedPreferences = null;

    public DiagKeySyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(getClass().getSimpleName(), "Starting to work ... :-)");

        boolean lockSuccess = lockConcurrency();
        if(!lockSuccess) {
            // another worker is already running -> abort current work
            Log.i(getClass().getSimpleName(), "Another worker is already in progress, so aborting work for now!");
            return Result.failure();
        }

        try {
            List<Country> countries = new ArrayList<>(Arrays.asList(Country.countries));

            boolean success = downloadDailyKeys(countries);

            Log.i(getClass().getSimpleName(), "Downloading is done with success: " + success);

            success &= checkDiagKeys();

            Log.i(getClass().getSimpleName(), "Work is done with success: " + success);
            if (success) {
                String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date());

                sharedPreferences
                        .edit()
                        .putString(getApplicationContext().getString(R.string.internal_settings_key_last_scan_timestamp), date)
                        .apply();

                // trigger update of risk dialog via intent
                Intent sndGetUpdates = new Intent(getApplicationContext(), DiagKeySyncService.class);
                sndGetUpdates.setAction(DiagKeySyncService.INTENT_GET_UPDATES);
                getApplicationContext().startService(sndGetUpdates);

                return Result.success();
            } else {
                return Result.failure();
            }
        } finally {
            unlockConcurrency();
        }
    }

    private boolean downloadDailyKeys(List<Country> countries) {
        boolean success = true;
        HashSet<String> availableCountries = new HashSet<>();
        for(Country country : countries) {
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
        String[] availableDates = country.getAvailableDates();
        if(availableDates == null) {
            // error fetching dates!
            return false;
        }

        // list all files in database
        HashSet<String> dbAvailableFiles = new HashSet<>();
        List<CwaCountryFile> cwaCountryFiles = db.runSync(() -> db.cwaDatabase().cwaCountryFile().getAllForCountry(cwaCountryId));
        for(CwaCountryFile cwaCountryFile : cwaCountryFiles) {
            dbAvailableFiles.add(cwaCountryFile.filename);
        }

        // fetch new days
        boolean success = true;
        for(String availableDate : availableDates) {
            if(!dbAvailableFiles.contains(availableDate)) {
                // import day
                success &= downloadDailyKeysForCountryForDay(country, cwaCountryId, availableDate);
            } else {
                dbAvailableFiles.remove(availableDate);
            }
        }

        // delete old days
        for(String removeFile : dbAvailableFiles) {
            db.runSync((Database.RunnableWithReturn<Void>) () -> {
                db.cwaDatabase().cwaCountryFile().deleteForCountry(cwaCountryId, removeFile);
                return null;
            });
        }

        return success;
    }

    private boolean downloadDailyKeysForCountryForDay(Country country, long countryId, String day) {
        Log.d(getClass().getSimpleName(), "parsing export.bin ...");
        TemporaryExposureKeyExportParser.TemporaryExposureKeyExport tek;
        try {
            tek = country.getParsedKeysForDate(day);
        } catch (Country.CountryDownloadException e) {
            Log.e(getClass().getSimpleName(), "Error downloading " + country.getCountryCode() + " -> " + day + ": " + e.getLocalizedMessage());
            return false;
        }
        Log.d(getClass().getSimpleName(), "parsing export.bin DONE");

        Database db = Database.getInstance(getApplicationContext());

        Log.d(getClass().getSimpleName(), "creating file in DB ...");
        // insert new file in db
        CwaCountryFile cwaCountryFile = new CwaCountryFile();
        cwaCountryFile.countryId = countryId;
        cwaCountryFile.filename = day;
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

        return true;
    }

    private boolean checkDiagKeys() {
        Database db = Database.getInstance(getApplicationContext());

        CwaTokenMinRollingTimestamp cwaTokenMinRollingTimestamp = db.runSync(() -> db.cwaDatabase().cwaToken().getMinRollingTimestamp());
        if(cwaTokenMinRollingTimestamp == null) return true;

        boolean success = true;

        // group diag keys by timeslots
        List<CwaDiagKey> diagKeyList = db.runSync(() -> db.cwaDatabase().cwaDiagKey().getUncheckedInRollingSection(cwaTokenMinRollingTimestamp.minRollingTimestamp));
        TimeslotDiagKeys timeslot = null;
        for(CwaDiagKey diagKey : diagKeyList) {
            if(timeslot != null && !timeslot.belongsToThisTimeslot(diagKey)) {
                success &= checkDiagKeysForTimeslot(timeslot);
                timeslot = null;
            }
            if(timeslot == null) timeslot = new TimeslotDiagKeys(diagKey);
            timeslot.addDiagKey(diagKey);
        }
        if(timeslot != null) success &= checkDiagKeysForTimeslot(timeslot);

        return success;
    }

    private boolean checkDiagKeysForTimeslot(TimeslotDiagKeys timeslotDiagKeys) {
        Database db = Database.getInstance(getApplicationContext());

        boolean success = true;

        List<CwaToken> tokenList = db.runSync(() -> db.cwaDatabase().cwaToken().getRollingSection(timeslotDiagKeys.referenceDiagKey.rollingStartIntervalNumber, timeslotDiagKeys.referenceDiagKey.rollingStartIntervalNumber + timeslotDiagKeys.referenceDiagKey.rollingPeriod));
        TimeslotTokens timeslotTokens = null;
        for(CwaToken token : tokenList) {
            if (timeslotTokens != null && !timeslotTokens.belongsToThisTimeslot(token)) {
                success &= checkDiagKeysForOnePeriod(timeslotDiagKeys, timeslotTokens);
                timeslotTokens = null;
            }
            if (timeslotTokens == null) timeslotTokens = new TimeslotTokens(token);
            timeslotTokens.addToken(token);
        }
        if(timeslotTokens != null) success &= checkDiagKeysForOnePeriod(timeslotDiagKeys, timeslotTokens);

        return success;
    }

    private boolean checkDiagKeysForOnePeriod(TimeslotDiagKeys timeslotDiagKeys, TimeslotTokens timeslotTokens) {
        List<CwaToken> updateTokens = new ArrayList<>();
        List<CwaDiagKey> updateDiagKeys = new ArrayList<>();

        boolean success = true;

        try {
            for (DiagKeyCrypto diagKeyCrypto : timeslotDiagKeys.getDiagKeys()) {
                CwaDiagKey diagKey = diagKeyCrypto.getDiagKey();
                for (CwaToken token : timeslotTokens.getTokens()) {
                    if(token.diagKey != null) continue;
                    if (diagKeyCrypto.isTokenMatching(token.token, token.rollingTimestamp)) {
                        // matching key!
                        token.diagKey = diagKey.id;
                        updateTokens.add(token);
                    }
                }

                diagKey.flagChecked = true;
                updateDiagKeys.add(diagKey);
            }
        } catch(DiagKeyCrypto.CryptoError e) {
            Log.e(getClass().getSimpleName(), "Checking diagnosis keys not possible. Crypto error: " + e);
            success = false;
        }

        Database db = Database.getInstance(getApplicationContext());
        if(!updateTokens.isEmpty()) {
            db.runSync(() -> db.cwaDatabase().cwaToken().update(updateTokens));
        }
        if(!updateDiagKeys.isEmpty()) {
            db.runSync(() -> db.cwaDatabase().cwaDiagKey().update(updateDiagKeys));
        }

        return success;
    }
}
