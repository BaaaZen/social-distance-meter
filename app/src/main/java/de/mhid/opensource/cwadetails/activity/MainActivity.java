package de.mhid.opensource.cwadetails.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.mhid.opensource.cwadetails.R;
import de.mhid.opensource.cwadetails.ble.BleScanService;

public class MainActivity extends AppCompatActivity {

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

    // start scanner service
    Intent bleServiceIntent = new Intent(this, BleScanService.class);
    startService(bleServiceIntent);

    // register user count from service
    IntentFilter rcvUserCountFilter = new IntentFilter();
    rcvUserCountFilter.addAction(BleScanService.INTENT_SCAN_RESULT_COUNT);
    BroadcastReceiver rcvUserCount = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if(intent != null) {
          int count = intent.getIntExtra("count", 0);
          updateUserCount(count);
        }
      }
    };
    registerReceiver(rcvUserCount, rcvUserCountFilter);
  }

  private void updateUserCount(int count) {
    // update text view
    TextView currentUsers = (TextView)findViewById(R.id.current_users_count);
    currentUsers.setText(getResources().getQuantityString(R.plurals.current_users_count, count, count));

    // update user icon
    int iconRes;
    if(count == 0) {
      iconRes = R.drawable.round_person_outline_24;
    } else if(count == 1) {
      iconRes = R.drawable.round_person_24;
    } else if(count == 2) {
      iconRes = R.drawable.round_people_24;
    } else {
      iconRes = R.drawable.round_groups_24;
    }
    ImageView currentUserIcon = (ImageView)findViewById(R.id.current_users_icon);
    Drawable icon;
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      icon = getDrawable(iconRes);
    } else {
      icon = getResources().getDrawable(iconRes);
    }
    currentUserIcon.setImageDrawable(icon);

    // set "card_current_waiting" invisible
    findViewById(R.id.card_current_waiting).setVisibility(View.GONE);
    // set "card_current_info" visible
    findViewById(R.id.card_current_info).setVisibility(View.VISIBLE);
  }
}