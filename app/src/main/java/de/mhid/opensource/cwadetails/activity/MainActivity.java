package de.mhid.opensource.cwadetails.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.mhid.opensource.cwadetails.R;
import de.mhid.opensource.cwadetails.services.BleScanService;
import de.mhid.opensource.cwadetails.services.DiagKeySyncService;
import de.mhid.opensource.cwadetails.views.HistoryGraphView;

public class MainActivity extends AppCompatActivity {
  public static final String INTENT_REQUEST_PERMISSION = "request_permission";
  public static final String INTENT_SCAN_RESULT_COUNT = "scan_result_count";
  public static final String INTENT_SCAN_RESULT_COUNT__COUNT = "count";

  public static final int COUNT_ERROR_SCANNING_IN_PROGRESS = -1;
  public static final int COUNT_ERROR_UNABLE_TO_SCAN = -2;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    // handle menu item selected
    int selectedId = item.getItemId();

    if(selectedId == R.id.menu_main_settings) {
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
    toolBarLayout.setTitle(getTitle());

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
      }
    });

    // TODO: TEST
    CardView cardCurrent = (CardView)findViewById(R.id.card_current);
    cardCurrent.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent diagKeyUpdateIntent = new Intent(MainActivity.this, DiagKeySyncService.class);
        diagKeyUpdateIntent.setAction(DiagKeySyncService.INTENT_START_DIAG_KEY_UPDATE);
        startService(diagKeyUpdateIntent);

        HistoryGraphView hgv = (HistoryGraphView)findViewById(R.id.current_history);
        hgv.update();
      }
    });

    // click handler for permission card
    CardView cardPermissions = (CardView)findViewById(R.id.card_permissions);
    cardPermissions.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        requestPermission();
      }
    });

    // register scan result receiver from service
    IntentFilter rcvUserCountFilter = new IntentFilter();
    rcvUserCountFilter.addAction(INTENT_SCAN_RESULT_COUNT);
    BroadcastReceiver rcvUserCount = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if(intent != null) {
          int count = intent.getIntExtra(INTENT_SCAN_RESULT_COUNT__COUNT, -1);
          updateUserCount(count);
        }
      }
    };
    registerReceiver(rcvUserCount, rcvUserCountFilter);

    // register request permission from service
    IntentFilter rcvRequestPermissionFilter = new IntentFilter();
    rcvRequestPermissionFilter.addAction(INTENT_REQUEST_PERMISSION);
    BroadcastReceiver rcvRequestPermission = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        checkRequestPermission();
      }
    };
    registerReceiver(rcvRequestPermission, rcvRequestPermissionFilter);

    // start scanner service and request recent user count
    Intent bleServiceIntent = new Intent(this, BleScanService.class);
    bleServiceIntent.setAction(BleScanService.INTENT_START_MAIN_ACTIVITY);
    startService(bleServiceIntent);

    // start diag key updates
    Intent diagKeyUpdateServiceIntent = new Intent(this, DiagKeySyncService.class);
    startService(diagKeyUpdateServiceIntent);
  }

  private void requestUserCount() {
    // start scanner service and request recent user count
    Intent bleServiceIntent = new Intent(this, BleScanService.class);
    bleServiceIntent.setAction(BleScanService.INTENT_START_MAIN_ACTIVITY);
    startService(bleServiceIntent);
  }

  public static String getStatusForUserCount(Context ctx, int count) {
    if(count == COUNT_ERROR_UNABLE_TO_SCAN) {
      // unable to scan
      return ctx.getResources().getString(R.string.card_current_users_unknown);
    } else if(count == COUNT_ERROR_SCANNING_IN_PROGRESS) {
      // scan in progress
      return ctx.getResources().getString(R.string.card_current_waiting_scan_results);
    } else if(count >= 0) {
      // valid user count -> update output
      return ctx.getResources().getQuantityString(R.plurals.card_current_users_count, count, count);
    } else {
      return ctx.getResources().getString(R.string.unknown_error);
    }
  }

  public static int getIconForUserCount(Context ctx, int count) {
    int iconRes;
    if(count == COUNT_ERROR_SCANNING_IN_PROGRESS) {
      iconRes = R.drawable.round_search_24;
    } else if(count == 0) {
      iconRes = R.drawable.round_person_outline_24;
    } else if(count == 1) {
      iconRes = R.drawable.round_person_24;
    } else if(count == 2) {
      iconRes = R.drawable.round_people_24;
    } else if(count > 2){
      iconRes = R.drawable.round_groups_24;
    } else {
      iconRes = R.drawable.round_error_outline_24;
    }
    return iconRes;
  }

  private void updateUserCount(int count) {
    // update text view
    TextView currentUsers = (TextView)findViewById(R.id.current_users_count);
    currentUsers.setText(getStatusForUserCount(this, count));

    // update user icon
    ImageView currentUserIcon = (ImageView)findViewById(R.id.current_users_icon);
    int iconRes = getIconForUserCount(this, count);
    Drawable icon;
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      icon = getDrawable(iconRes);
    } else {
      icon = getResources().getDrawable(iconRes);
    }
    currentUserIcon.setImageDrawable(icon);
  }

  private void checkRequestPermission() {
    CardView cardPermissions = (CardView)findViewById(R.id.card_permissions);
    TextView permissionDescription = (TextView)findViewById(R.id.card_permissions_description);
    TextView permissionCommand = (TextView)findViewById(R.id.card_permissions_command);

    String nationalCWAName = getResources().getString(R.string.national_cwa_name);
    String backgroundPermissionOptionLabel = getResources().getString(R.string.card_permissions_label_background_permission);
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      backgroundPermissionOptionLabel = getPackageManager().getBackgroundPermissionOptionLabel().toString();
    }

    if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // only request location
        permissionDescription.setText(getResources().getString(R.string.card_permissions_description_location, nationalCWAName));
        permissionCommand.setText(getResources().getString(R.string.card_permissions_command_location));
      } else {
        // request location + background
        permissionDescription.setText(getResources().getString(R.string.card_permissions_description_location_background, nationalCWAName, backgroundPermissionOptionLabel));
        permissionCommand.setText(getResources().getString(R.string.card_permissions_command_location_background, backgroundPermissionOptionLabel));
      }
      cardPermissions.setVisibility(View.VISIBLE);
    } else if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      // request background
      permissionDescription.setText(getResources().getString(R.string.card_permissions_description_background, backgroundPermissionOptionLabel));
      permissionCommand.setText(getResources().getString(R.string.card_permissions_command_background, backgroundPermissionOptionLabel));
      cardPermissions.setVisibility(View.VISIBLE);
    } else {
      // nothing to request -> hide
      cardPermissions.setVisibility(View.GONE);
    }
  }

  private void requestPermission() {
    if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      final String[] permissionsLocation = new String[] { Manifest.permission.ACCESS_FINE_LOCATION };
      ActivityCompat.requestPermissions(this, permissionsLocation, 1);
    } else if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      final String[] permissionsBackgroundLocation = new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION };
      ActivityCompat.requestPermissions(this, permissionsBackgroundLocation, 1);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    // reset user count
    requestUserCount();
    // re-check permissions
    checkRequestPermission();
  }
}