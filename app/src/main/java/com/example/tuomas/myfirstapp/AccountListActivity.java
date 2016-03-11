package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

// DONE: Disable underlying GUI controls when Add/Edit view is visible / enable afterwards
// DONE: Store IBANs in database with grouping spaces - easier that way
// DONE: Account data input and save ("Add" button, input popup, filtering / errormsg, save to db)
// DONE: Account item edit (edit, delete)
// DONE: Add/edit duplicate confirmation / replace functionality (just change save button text and show instruction?)
// TODO: Implement state saving and retrieval
// TODO: Setup git repo and push
// TODO: Test if everything required is in the project folder
// TODO: Comment code
// TODO: Other orientations and screen sizes, hopefully got time for this (in my dreams...)
// TODO: Convert DB operations into proper style (could use more _id instead of name)
// TODO: Add missing rare IBAN CCs and lengths, see if could place them to XML
// TODO: Animate / add other states to Add button
// SKIP: Make db query async? Was REALLY slow at some point, blocking UI, but later appears to work ok
public class AccountListActivity extends Activity {

  private DataManager mDataManager;
  private EventManager mEventManager;
  private GuiManager mGuiManager;
  private boolean isFirstRun;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d("A", "onCreate start");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    isFirstRun = savedInstanceState == null;

    // Setup references
    mGuiManager = GuiManager.get();
    mDataManager = DataManager.get();
    mEventManager = EventManager.get();

    mGuiManager.setActivity(this);
    mDataManager.setActivity(this);
    mEventManager.setActivity(this);

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
    Log.d("A", "onCreate end");
  }

  @Override
  public void onResume() {
    Log.d("A", "onResume");
    super.onResume();
    mDataManager.openDatabase();

    // this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    //mEventManager.hideSoftKeyboard(this);
    if (isFirstRun) {
      // Generate ListView data from SQLite Database
      mDataManager.insertDebugData();
    }
    mDataManager.refreshAccountData();
  }

  @Override
  public void onPause() {
    super.onPause();
    // Better close db object on pause, the process may
    // just get killed by the system without a notice
    mDataManager.closeDatabase();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    // listviewBlockerButton[] visibilities
    // newEditAccountContainer visibility
    // if (visible)
  }
}
