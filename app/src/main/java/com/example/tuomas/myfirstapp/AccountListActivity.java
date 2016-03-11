package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.os.Bundle;

// DONE: Disable underlying GUI controls when Add/Edit view is visible / enable afterwards
// DONE: Store IBANs in database with grouping spaces - easier that way
// DONE: Account data input and save ("Add" button, input popup, filtering / errormsg, save to db)
// DONE: Account item edit (edit, delete)
// DONE: Add/edit duplicate confirmation / replace functionality (just change save button text and show instruction?)
// DONE: Implement state saving and retrieval
// DONE: Setup git repo and push
// TODO: Test if everything required is in the project folder
// TODO: Comment code
// TODO: MAKE SURE TO DISABLE DEBUGGING DATA RECREATION BEFORE SUBMITTING THE PROJECT!!!
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
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // Setup references
    mGuiManager = GuiManager.get();
    mDataManager = DataManager.get();
    mEventManager = EventManager.get();
    mGuiManager.updateReferences(this);
    mDataManager.updateReferences(this);
    mEventManager.updateReferences(this);

    // Setup event listeners
    mEventManager.setEventListeners();

    // Open database. NOTE: Called also in onResume(), because
    // database will be closed in onPause() in preparation of
    // accidental shutdown under low system resources. Normally
    // this should be early enough to initialize db connection.
    // However, when activity just gets restarted (screen orientation
    // change) database is sometimes accessed before connection
    // is opened in onResume() due to search bar onTextChanged
    // callback firing. This only seems to happen if reaching
    // openDatabase() in onResume() takes too long. System probably
    // does this in an attempt to recreate GUI input field values.
    // Anyway, better issue open command here as well to avoid crashes.
    mDataManager.openDatabase();
    mDataManager.restoreSession(savedInstanceState);
    if (savedInstanceState == null) {
      // First run -> generate ListView data from SQLite Database
      mDataManager.insertFreshDebugData();
    }
    mDataManager.refreshAccountScreenView();
  }

  @Override
  public void onResume() {
    super.onResume();
    mDataManager.openDatabase();

    // this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    //mEventManager.hideSoftKeyboard(this);

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
    SessionStorage storage = mDataManager.saveSession();
    outState.putSerializable("session", storage);
    super.onSaveInstanceState(outState);
    // Log.d("A", mGuiManager.mGuiState.toString() + ", " + );
    //outState.
    // listviewBlockerButton[] visibilities
    // newEditAccountContainer visibility
    // if (visible)
  }

}
