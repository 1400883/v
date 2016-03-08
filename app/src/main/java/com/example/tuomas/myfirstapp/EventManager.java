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
  private Activity mActivity;
  DataManager mDataManager;

  public EventManager(Activity activity, DataManager manager) {
    mActivity = activity;
    mDataManager = manager;
  }

  public void setEventListeners() {
    // Main screen
    ((ImageButton) mActivity.findViewById(R.id.addAccountButton)).setOnClickListener(this);
    ((EditText) mActivity.findViewById(R.id.searchBar)).addTextChangedListener(this);
    ((ListView) mActivity.findViewById(R.id.accountListview)).setOnItemClickListener(this);

    // Add / edit account view

    ((Button) mActivity.findViewById(R.id.saveButton)).setOnClickListener(this);
    ((Button) mActivity.findViewById(R.id.cancelButton)).setOnClickListener(this);
    ((Button) mActivity.findViewById(R.id.deleteButton)).setOnClickListener(this);
  }

  // SearchBar EditText listeners
  /////////////////////////////////////
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

  // ListView onItemClick listener
  /////////////////////////////////////
  @Override
  public void onItemClick(
    AdapterView<?> listView,
    View view,
    int position,
    long id) {
    // Get the cursor, positioned to the corresponding row in the result set
    Cursor cursor = (Cursor) listView.getItemAtPosition(position);

    // Get the state's capital from this row in the database.
    String countryCode =
      cursor.getString(
        cursor.getColumnIndexOrThrow(
          DatabaseAdapter.COLUMN_IBAN));
    Toast.makeText(mActivity.getApplicationContext(),
      countryCode, Toast.LENGTH_SHORT).show();
  }

  // New/Edit account button onClick listeners
  /////////////////////////////////////
  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.addAccountButton:
        // Add new account
        LinearLayout addAccountContainer =
          (LinearLayout)mActivity.findViewById(R.id.addEditAccountContainer);
        if (addAccountContainer.getVisibility() != View.VISIBLE) {
          ((TextView)mActivity.findViewById(R.id.newEditAccountTitle))
            .setText(R.string.NewAccount_title);
          ((TextView)mActivity.findViewById(R.id.nameErrorMessage)).setVisibility(View.INVISIBLE);
          ((EditText)mActivity.findViewById(R.id.nameInputField)).setText("");
          ((EditText)mActivity.findViewById(R.id.ibanInputField)).setText("");
          addAccountContainer.setVisibility(View.VISIBLE);
        }
        break;
      case R.id.saveButton:
        // Save
        EditText et = (EditText) mActivity.findViewById(R.id.nameInputField);
        String a = "abcd";
        et.setText(a);
        break;
      case R.id.cancelButton:
        // Cancel
        break;
      case R.id.deleteButton:
        // Delete
        break;
      default:
        break;
    }
  }
}
