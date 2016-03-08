package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.os.Bundle;

// DONE: Disable underlying GUI controls when Add/Edit view is visible / enable afterwards

// TODO: Store IBANs in database with grouping spaces - easier that way
// TODO: Add new account image button style matching
// TODO: Account data input and save ("Add" button, input popup, filtering / errormsg, save to db)
// TODO: Account item edit (edit, delete, copy to clipboard)
// TODO: Add/edit duplicate confirmation / replace functionality (just change save button text and show instruction?)
// TODO: Make db query async? Was REALLY slow at some point, blocking UI but now that I tried works ok
// TODO: other orientations and screen sizes, hopefully got time for this
public class AccountListActivity extends Activity {

  private DataManager mDataManager;
  private EventManager mEventManager;
  private GuiManager mGuiManager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

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
  }

  @Override
  public void onResume() {
    super.onResume();
    mDataManager.openDatabase();
    // Generate ListView from SQLite Database
    mDataManager.populateData();
  }

  @Override
  public void onPause() {
    super.onPause();
    // Better close db object on pause, the process may
    // just get killed by the system without a notice
    mDataManager.closeDatabase();
  }
}
