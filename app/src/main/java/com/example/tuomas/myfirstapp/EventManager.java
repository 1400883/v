package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
  }

  ///////////////////////////////////////////////
  // SearchBar EditText listeners
  ///////////////////////////////////////////////
  @Override
  public void afterTextChanged(Editable s) {
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
  }

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
        mGuiManager.onAddAccountButton();
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
