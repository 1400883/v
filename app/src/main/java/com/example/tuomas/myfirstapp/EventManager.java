package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

public class EventManager implements View.OnClickListener, TextWatcher, AdapterView.OnItemClickListener {
  private static Activity mActivity;
  private DataManager mDataManager;
  private GuiManager mGuiManager;
  private static EventManager instance = null;

  ///////////////////////////////////////////////
  // Singleton setup. Reference to activity and other managers
  // must be set before requesting instance via singleton
  ///////////////////////////////////////////////

  public static void setActivity(Activity activity) {
    mActivity = activity;
  }
  public void setManagers(DataManager dataManager, GuiManager guiManager) {
    mDataManager = dataManager;
    mGuiManager = guiManager;
  }

  public static EventManager get() {
    if (instance == null) {
      instance = new EventManager();
    }
    return instance;
  }

  ///////////////////////////////////////////////
  // Constructor
  ///////////////////////////////////////////////
  private EventManager() {}

  ///////////////////////////////////////////////

  ///////////////////////////////////////////////
  // Event listener setup method
  ///////////////////////////////////////////////
  public void setEventListeners() {
    // Main screen
    ((Button) mActivity.findViewById(R.id.addAccountButton)).setOnClickListener(this);
    ((EditText) mActivity.findViewById(R.id.searchField)).addTextChangedListener(this);
    ((ListView) mActivity.findViewById(R.id.listview)).setOnItemClickListener(this);

    // Add / edit account view
    ((Button) mActivity.findViewById(R.id.saveButton)).setOnClickListener(this);
    ((Button) mActivity.findViewById(R.id.cancelButton)).setOnClickListener(this);
    ((Button) mActivity.findViewById(R.id.deleteButton)).setOnClickListener(this);

    // Use listeners for new / edit account inputs only to clear potentially
    // visible error messages. This should improve usability a bit.
    ((EditText) mActivity.findViewById(R.id.nameInputField)).addTextChangedListener(
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) { mGuiManager.clearErrorMessage(); }
      }
    );
    ((EditText) mActivity.findViewById(R.id.ibanInputField)).addTextChangedListener(
      new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) { mGuiManager.clearErrorMessage(); }
      }
    );

    // Get rid of the mighty everlasting soft keyboard until explicitly called for
    LinearLayout rootContainer = (LinearLayout)mActivity.findViewById(R.id.rootContainer);
    keyboardHideListener(rootContainer);
  }

  private void keyboardHideListener(View view) {
    // Set up touch listeners for non-editable text views to hide soft keyboard
    if(!(view instanceof EditText)) {
      view.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
          hideSoftKeyboard(mActivity);
          return false;
        }
      });
    }

    // Go recursively into viewgroups to get all views. Painful, but works.
    if (view instanceof ViewGroup) {
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        View innerView = ((ViewGroup) view).getChildAt(i);
        keyboardHideListener(innerView);
      }
    }
  }

  private void hideSoftKeyboard(Activity activity) {
    InputMethodManager imManager =
      (InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
    imManager.hideSoftInputFromWindow(
      mActivity.getCurrentFocus().getWindowToken(), 0);
  }

  ///////////////////////////////////////////////
  // SearchBar EditText listeners
  ///////////////////////////////////////////////
  @Override
  public void afterTextChanged(Editable s) {}
  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    mDataManager.getCursorAdapter().getFilter().filter(s.toString());
  }

  ///////////////////////////////////////////////
  // ListView onItemClick listener
  ///////////////////////////////////////////////
  @Override
  public void onItemClick(
    AdapterView<?> listView,
    View view,
    int position,
    long id) {
    // Get the cursor, positioned to the corresponding row in the result set
    Cursor cursor = (Cursor) listView.getItemAtPosition(position);

    // Get the data from this row in the database.
    /*
    String countryCode =
      cursor.getString(
        cursor.getColumnIndexOrThrow(
          DataManager.DatabaseAdapter.COLUMN_IBAN));
    Toast.makeText(mActivity.getApplicationContext(),
      countryCode, Toast.LENGTH_SHORT).show();
     */
  }

  ///////////////////////////////////////////////
  // New/Edit account button onClick listeners
  ///////////////////////////////////////////////
  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.addAccountButton:
        mGuiManager.onNewAccountButton();
        break;
      case R.id.saveButton:
        mGuiManager.onSaveButton();
        break;
      case R.id.cancelButton:
        // Cancel
        mGuiManager.onCancelButton();
        break;
      case R.id.deleteButton:
        // Delete
        break;
      default:
        break;
    }
  }
}
