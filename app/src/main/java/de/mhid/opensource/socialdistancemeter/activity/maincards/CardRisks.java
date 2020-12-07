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
package de.mhid.opensource.socialdistancemeter.activity.maincards;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.Html;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.activity.MainActivity;
import de.mhid.opensource.socialdistancemeter.database.CwaTokenRisk;
import de.mhid.opensource.socialdistancemeter.database.Database;
import de.mhid.opensource.socialdistancemeter.services.DiagKeySyncService;

public class CardRisks {
    public static final String INTENT_SYNC_LAST_UPDATE = "sync_last_update";
    public static final String INTENT_SYNC_LAST_UPDATE__DATE = "date";

    public static final String INTENT_SYNC_DIAG_KEY_COUNT = "diag_key_count";
    public static final String INTENT_SYNC_DIAG_KEY_COUNT__COUNT = "count";

    public static final String INTENT_SYNC_STATUS_SYNC = "status_sync";
    public static final String INTENT_SYNC_STATUS_SYNC__ERROR = "error";
    public static final String INTENT_SYNC_STATUS_SYNC__RUNNING = "running";
    public static final String INTENT_SYNC_STATUS_SYNC__DESCRIPTION = "description";
    public static final String INTENT_SYNC_STATUS_SYNC__PROGRESS = "progress";

    public static final String INTENT_ENCOUNTERS_UPDATE = "encounters_update";

    private final MainActivity mainActivity;
    private final SharedPreferences sharedPreferences;

    private List<CwaTokenRisk> riskList = null;

    public CardRisks(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);

        init();
    }

    private void init() {
        registerClickListeners();
        registerIntentReceivers();
        registerSharedPreferenceChangeListener();

        triggerUpdates();

        updateEncounters(false);
        setSyncButtonVisibility();
    }

    private void triggerUpdates() {
        Intent sndGetUpdates = new Intent(mainActivity, DiagKeySyncService.class);
        sndGetUpdates.setAction(DiagKeySyncService.INTENT_GET_UPDATES);
        mainActivity.startService(sndGetUpdates);
    }

    private void registerIntentReceivers() {
        // register last update receiver from service
        IntentFilter rcvSyncLastUpdateFilter = new IntentFilter();
        rcvSyncLastUpdateFilter.addAction(INTENT_SYNC_LAST_UPDATE);
        BroadcastReceiver rcvSyncLastUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            if(intent != null) {
                if(intent.hasExtra(INTENT_SYNC_LAST_UPDATE__DATE)) {
                    String date = intent.getStringExtra(INTENT_SYNC_LAST_UPDATE__DATE);
                    setLastUpdateDetails(mainActivity.getString(R.string.card_risks_last_update_date, "<b>" + date + "</b>"));
                } else {
                    setLastUpdateDetails(mainActivity.getString(R.string.card_risks_last_update_never));
                }
            }
            }
        };
        mainActivity.registerReceiver(rcvSyncLastUpdate, rcvSyncLastUpdateFilter);

        // register diag key count receiver from service
        IntentFilter rcvDiagKeyCountFilter = new IntentFilter();
        rcvDiagKeyCountFilter.addAction(INTENT_SYNC_DIAG_KEY_COUNT);
        BroadcastReceiver rcvDiagKeyCount = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            if(intent != null) {
                int count = intent.getIntExtra(INTENT_SYNC_DIAG_KEY_COUNT__COUNT, 0);
                setDiagKeyCount(count);
            }
            }
        };
        mainActivity.registerReceiver(rcvDiagKeyCount, rcvDiagKeyCountFilter);

        // register sync status update
        IntentFilter rcvSyncStatusUpdateFilter = new IntentFilter();
        rcvSyncStatusUpdateFilter.addAction(INTENT_SYNC_STATUS_SYNC);
        BroadcastReceiver rcvSyncStatusUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent != null) {
                    boolean error = intent.getBooleanExtra(INTENT_SYNC_STATUS_SYNC__ERROR, false);
                    if(error) {
                        String description = intent.getStringExtra(INTENT_SYNC_STATUS_SYNC__DESCRIPTION);
                        setSyncError(description);
                        return;
                    }

                    boolean running = intent.getBooleanExtra(INTENT_SYNC_STATUS_SYNC__RUNNING, false);
                    if(running) {
                        String description = intent.getStringExtra(INTENT_SYNC_STATUS_SYNC__DESCRIPTION);
                        int progress = intent.getIntExtra(INTENT_SYNC_STATUS_SYNC__PROGRESS, 0);
                        setSyncRunning(description, progress);
                    } else {
                        setSyncNotRunning();
                    }
                }
            }
        };
        mainActivity.registerReceiver(rcvSyncStatusUpdate, rcvSyncStatusUpdateFilter);

        // register encounters update receiver from service
        IntentFilter rcvEncountersUpdateFilter = new IntentFilter();
        rcvDiagKeyCountFilter.addAction(INTENT_ENCOUNTERS_UPDATE);
        BroadcastReceiver rcvEncountersUpdate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent != null) {
                    updateEncounters(true);
                }
            }
        };
        mainActivity.registerReceiver(rcvEncountersUpdate, rcvEncountersUpdateFilter);
    }

    private void registerClickListeners() {
        ConstraintLayout blockStartSync = mainActivity.findViewById(R.id.card_risks_start_sync_inner);
        blockStartSync.setOnClickListener(v -> {
            Intent diagKeyUpdateIntent = new Intent(mainActivity, DiagKeySyncService.class);
            diagKeyUpdateIntent.setAction(DiagKeySyncService.INTENT_START_DIAG_KEY_UPDATE);
            mainActivity.startService(diagKeyUpdateIntent);
        });
    }

    private void registerSharedPreferenceChangeListener() {
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals(mainActivity.getString(R.string.settings_key_risk_sync_enabled))) {
                    setSyncButtonVisibility();
                }
            }
        };
    }

    private void setSyncButtonVisibility() {
        boolean syncEnabled = sharedPreferences.getBoolean(mainActivity.getString(R.string.settings_key_risk_sync_enabled), true);
        setSyncButtonVisibility(syncEnabled);
    }

    private void setSyncButtonVisibility(boolean visible) {
        LinearLayout startSyncBlock = mainActivity.findViewById(R.id.card_risks_start_sync);
        startSyncBlock.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setLastUpdateDetails(String s) {
        TextView lastUpdateDetails = mainActivity.findViewById(R.id.card_risks_last_update_details);
        lastUpdateDetails.setText(Html.fromHtml(s));
    }

    private void setDiagKeyCount(int count) {
        ConstraintLayout blockDiagKeyCount = mainActivity.findViewById(R.id.card_risks_diag_keys);
        if(count < 1) {
            blockDiagKeyCount.setVisibility(View.GONE);
        } else {
            TextView diagKeyDetails = mainActivity.findViewById(R.id.card_risks_diag_keys_details);
            String s = mainActivity.getString(R.string.card_risks_diag_key_count, "<b>" + count + "</b>");
            diagKeyDetails.setText(Html.fromHtml(s));

            blockDiagKeyCount.setVisibility(View.VISIBLE);
        }
    }

    private void setSyncNotRunning() {
        ConstraintLayout syncStatusBlock = mainActivity.findViewById(R.id.card_risks_sync);
        syncStatusBlock.setVisibility(View.GONE);

        ImageView syncAnimation = mainActivity.findViewById(R.id.card_risks_sync_icon);
        syncAnimation.clearAnimation();

        ConstraintLayout syncButton = mainActivity.findViewById(R.id.card_risks_start_sync_inner);
        syncButton.setVisibility(View.VISIBLE);
    }

    private void setSyncRunning(String description, int progress) {
        ConstraintLayout syncButton = mainActivity.findViewById(R.id.card_risks_start_sync_inner);
        syncButton.setVisibility(View.GONE);

        ImageView syncAnimation = mainActivity.findViewById(R.id.card_risks_sync_icon);
        syncAnimation.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.round_sync_24));
        Animation animatedSyncAnimation = AnimationUtils.loadAnimation(mainActivity, R.anim.rotate_ccw);
        animatedSyncAnimation.setRepeatCount(Animation.INFINITE);
        syncAnimation.startAnimation(animatedSyncAnimation);

        TextView syncDescription = mainActivity.findViewById(R.id.card_risks_sync_details);
        syncDescription.setText(description);

        ProgressBar syncProgress = mainActivity.findViewById(R.id.card_risks_sync_progress);
        syncProgress.setVisibility(View.VISIBLE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            syncProgress.setProgress(progress, progress > 0);
        } else {
            syncProgress.setProgress(progress);
        }

        ConstraintLayout syncStatusBlock = mainActivity.findViewById(R.id.card_risks_sync);
        syncStatusBlock.setVisibility(View.VISIBLE);
    }

    private void setSyncError(String error) {
        ConstraintLayout syncStatusBlock = mainActivity.findViewById(R.id.card_risks_sync);
        syncStatusBlock.setVisibility(View.VISIBLE);

        ProgressBar syncProgress = mainActivity.findViewById(R.id.card_risks_sync_progress);
        syncProgress.setVisibility(View.GONE);

        TextView syncDescription = mainActivity.findViewById(R.id.card_risks_sync_details);
        syncDescription.setText(error);

        ImageView syncAnimation = mainActivity.findViewById(R.id.card_risks_sync_icon);
        syncAnimation.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.round_sync_problem_24));
        syncAnimation.clearAnimation();

        ConstraintLayout syncButton = mainActivity.findViewById(R.id.card_risks_start_sync_inner);
        syncButton.setVisibility(View.VISIBLE);
    }

    private void updateEncounters(boolean force) {
        List<CwaTokenRisk> riskList = getRiskList();
        if(force || riskList == null) {
            ConstraintLayout encountersNoRisk = mainActivity.findViewById(R.id.card_risks_encounters_no_risk);
            encountersNoRisk.setVisibility(View.GONE);

            ConstraintLayout encountersDetails = mainActivity.findViewById(R.id.card_risks_encounters_details);
            encountersDetails.setVisibility(View.GONE);

            ConstraintLayout encountersWaiting = mainActivity.findViewById(R.id.card_risks_encounters_waiting);
            encountersWaiting.setVisibility(View.VISIBLE);

            Database db = Database.getInstance(mainActivity);
            db.runAsync(() -> {
                final List<CwaTokenRisk> riskList1 = db.cwaDatabase().cwaToken().getRisks();
                setRiskList(riskList1);
                updateEncountersUiFromList(riskList1);
            });
        } else {
            updateEncountersUiFromList(riskList);
        }
    }

    private void updateEncountersUiFromList(List<CwaTokenRisk> riskList) {
        mainActivity.runOnUiThread(() -> {
            ConstraintLayout encountersWaiting = mainActivity.findViewById(R.id.card_risks_encounters_waiting);
            encountersWaiting.setVisibility(View.GONE);

            ConstraintLayout encountersNoRisk = mainActivity.findViewById(R.id.card_risks_encounters_no_risk);
            ConstraintLayout encountersDetails = mainActivity.findViewById(R.id.card_risks_encounters_details);
            if(riskList == null || riskList.size() < 1) {
                encountersNoRisk.setVisibility(View.VISIBLE);
                encountersDetails.setVisibility(View.GONE);
            } else {
                TextView encountersCount = mainActivity.findViewById(R.id.card_risks_encounters_details_count_details);
                String sCount = mainActivity.getString(R.string.card_risks_encounters_details_count, "<b>" + riskList.size() + "</b>");
                encountersCount.setText(Html.fromHtml(sCount));

                TextView encountersRecent = mainActivity.findViewById(R.id.card_risks_encounters_details_recent_details);
                long maxTimestamp = 0;
                for(CwaTokenRisk risk : riskList) {
                    if(risk.maxLocalTimestamp > maxTimestamp) maxTimestamp = risk.maxLocalTimestamp;
                }
                Date d = new Date(maxTimestamp);
                String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(d);
                String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(d);
                String dateTime = mainActivity.getString(R.string.date_time_format, date, time);
                String sRecent = mainActivity.getString(R.string.card_risks_encounters_details_recent, "<b>" + dateTime + "</b>");
                encountersRecent.setText(Html.fromHtml(sRecent));

                encountersNoRisk.setVisibility(View.GONE);
                encountersDetails.setVisibility(View.VISIBLE);
            }
        });
    }

    private synchronized void setRiskList(List<CwaTokenRisk> riskList) {
        this.riskList = riskList;
    }

    private synchronized List<CwaTokenRisk> getRiskList() {
        return riskList;
    }
}
