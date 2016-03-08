package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

// TODO: Store IBANs in database with grouping spaces - easier that way
// TODO: Account data input and save ("Add" button, input popup, filtering / errormsg, save to db)
// TODO: Disable underlying GUI controls when Add/Edit view is visible / enable afterwards
// TODO: Account item edit (edit, delete, copy to clipboard)
// TODO: Add/edit duplicate confirmation
// TODO: Make db query async

public class AccountListActivity extends Activity {

  private DataManager mDataManager;
  private EventManager mEventManager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    mDataManager = new DataManager(this);
    mEventManager = new EventManager(this, mDataManager);
    mEventManager.setEventListeners();
    mDataManager.setListviewFilter();
    mDataManager.setListviewDataSource(this);
    setListviewFrameLayoutParameters();
  }

  @Override
  public void onResume() {
    super.onResume();
    mDataManager.openDatabase();
    // Generate ListView from SQLite Database
    mDataManager.fillListview();
  }

  @Override
  public void onPause() {
    super.onPause();
    // Better close db object on pause, the process may
    // just get killed by the system without a notice
    mDataManager.closeDatabase();
  }

  private void setListviewFrameLayoutParameters() {
    // For whatever reason, FrameLayout won't respect XML layout
    // MATCH_PARENT parameters, but insist on WRAP_CONTENT.
    // Setting them here fixes it.
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.MATCH_PARENT);
    FrameLayout f = (FrameLayout) findViewById(R.id.frameLayout);
    f.setLayoutParams(p);
  }
}
