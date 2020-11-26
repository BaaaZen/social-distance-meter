package de.mhid.opensource.socialdistancemeter.activity.maincards;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.RotateDrawable;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import de.mhid.opensource.socialdistancemeter.R;
import de.mhid.opensource.socialdistancemeter.activity.MainActivity;
import de.mhid.opensource.socialdistancemeter.services.DiagKeySyncService;

public class CardRisks {
    public static final String INTENT_SYNC_LAST_UPDATE = "sync_last_update";
    public static final String INTENT_SYNC_LAST_UPDATE__DATE = "date";

    public static final String INTENT_SYNC_DIAG_KEY_COUNT = "diag_key_count";
    public static final String INTENT_SYNC_DIAG_KEY_COUNT__COUNT = "count";

    public static final String INTENT_SYNC_STATUS_SYNC = "status_sync";
    public static final String INTENT_SYNC_STATUS_SYNC__RUNNING = "running";
    public static final String INTENT_SYNC_STATUS_SYNC__DESCRIPTION = "description";
    public static final String INTENT_SYNC_STATUS_SYNC__PROGRESS = "progress";

    private MainActivity mainActivity;

    public CardRisks(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        init();
    }

    private void init() {
        registerClickListeners();
        registerIntentReceivers();

        triggerUpdates();
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
                    setLastUpdateDetails(mainActivity.getString(R.string.card_risks_last_update_date, date));
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
    }

    private void registerClickListeners() {
        ConstraintLayout blockStartSync = mainActivity.findViewById(R.id.card_risks_start_sync);
        blockStartSync.setOnClickListener(v -> {
            Intent diagKeyUpdateIntent = new Intent(mainActivity, DiagKeySyncService.class);
            diagKeyUpdateIntent.setAction(DiagKeySyncService.INTENT_START_DIAG_KEY_UPDATE);
            mainActivity.startService(diagKeyUpdateIntent);
        });
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
            String s = mainActivity.getString(R.string.card_risks_diag_key_count, count);
            diagKeyDetails.setText(Html.fromHtml(s));

            blockDiagKeyCount.setVisibility(View.VISIBLE);
        }
    }

    private void setSyncNotRunning() {
        ConstraintLayout syncStatusBlock = mainActivity.findViewById(R.id.card_risks_sync);
        syncStatusBlock.setVisibility(View.GONE);

//        ImageView syncAnimation = mainActivity.findViewById(R.id.card_risks_sync_icon);
//        AnimationDrawable animationDrawable = (AnimationDrawable) syncAnimation.getDrawable();
//        animationDrawable.stop();

        ConstraintLayout syncButton = mainActivity.findViewById(R.id.card_risks_start_sync);
        syncButton.setVisibility(View.VISIBLE);
    }

    private void setSyncRunning(String description, int progress) {
        ConstraintLayout syncButton = mainActivity.findViewById(R.id.card_risks_start_sync);
        syncButton.setVisibility(View.GONE);

//        ImageView syncAnimation = mainActivity.findViewById(R.id.card_risks_sync_icon);
//        AnimationDrawable animationDrawable = (AnimationDrawable)syncAnimation.getDrawable();
//        animationDrawable.start();

        TextView syncDescription = mainActivity.findViewById(R.id.card_risks_sync_details);
        syncDescription.setText(description);

        ProgressBar syncProgress = mainActivity.findViewById(R.id.card_risks_sync_progress);
        syncProgress.setProgress(progress);

        ConstraintLayout syncStatusBlock = mainActivity.findViewById(R.id.card_risks_sync);
        syncStatusBlock.setVisibility(View.VISIBLE);
    }
}
