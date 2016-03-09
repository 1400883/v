package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

// DONE: Disable underlying GUI controls when Add/Edit view is visible / enable afterwards
// DONE: Store IBANs in database with grouping spaces - easier that way
// DONE: Account data input and save ("Add" button, input popup, filtering / errormsg, save to db)

// TODO: Account item edit (edit, delete, copy to clipboard)
// TODO: (Add DONE) Add/edit duplicate confirmation / replace functionality (just change save button text and show instruction?)
// TODO: Make db query async? Was REALLY slow at some point, blocking UI, but later appears to work ok
// TODO: other orientations and screen sizes, hopefully got time for this
// TODO: Animate / add other states to Add button

public class AccountListActivity extends Activity {

  private DataManager mDataManager;
  private EventManager mEventManager;
  private GuiManager mGuiManager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    Log.v("onCreate", "asdf");
    // Setup references
    GuiManager.setActivity(this);
    DataManager.setActivity(this);
    EventManager.setActivity(this);
    mGuiManager = GuiManager.get();
    mDataManager = DataManager.get();
    mEventManager = EventManager.get();
    mGuiManager.setManagers(mEventManager, mDataManager);
    mDataManager.setManagers(mEventManager, mGuiManager);
    mEventManager.setManagers(mDataManager, mGuiManager);

    // Setup event listeners
    mEventManager.setEventListeners();
    // Activate listview filtering based on search bar content
    mDataManager.setResultFilter();
    // Set listview data source
    mDataManager.setDataSource();

    mDataManager.openDatabase();
  }

  @Override
  public void onResume() {
    Log.v("onResume", "ryth");
    super.onResume();
    mDataManager.openDatabase();
    // Generate ListView from SQLite Database
    mDataManager.populateData();
  }

  @Override
  public void onPause() {
    Log.v("onPause", "");
    super.onPause();
    // Better close db object on pause, the process may
    // just get killed by the system without a notice
    mDataManager.closeDatabase();
  }
}
