package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.database.Cursor;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

public class EventManager implements
  View.OnClickListener, TextWatcher, AdapterView.OnItemClickListener {

  private Activity mActivity = null;
  private DataManager mDataManager;
  private GuiManager mGuiManager;
  private static EventManager instance = null;
  ///////////////////////////////////////////////
  // Singleton
  ///////////////////////////////////////////////
  public static EventManager get() {
    if (instance == null) {
      instance = new EventManager();
    }
    return instance;
  }
  private EventManager() {}
  public void updateReferences(Activity activity) {
    if (mActivity != activity) {
      mActivity = activity;
      if (mDataManager == null || mGuiManager == null) {
        mDataManager = DataManager.get();
        mGuiManager = GuiManager.get();
      }
    }
  }
  ///////////////////////////////////////////////
  // Event listeners
  ///////////////////////////////////////////////
  public void setEventListeners() {
    // Main screen
    mActivity.findViewById(R.id.addAccountButton).setOnClickListener(this);
    ((EditText) mActivity.findViewById(R.id.searchField)).addTextChangedListener(this);
    ((ListView) mActivity.findViewById(R.id.listview)).setOnItemClickListener(this);

    // Add / edit account view
    mActivity.findViewById(R.id.saveButton).setOnClickListener(this);
    mActivity.findViewById(R.id.cancelButton).setOnClickListener(this);
    mActivity.findViewById(R.id.deleteButton).setOnClickListener(this);

    // Use listeners for new / edit account inputs only to clear potentially
    // visible error messages. This should improve usability a bit.
    ((EditText) mActivity.findViewById(R.id.nameInputField))
      .addTextChangedListener(new CustomTextWatcher());
    ((EditText) mActivity.findViewById(R.id.ibanInputField))
      .addTextChangedListener(new CustomTextWatcher());

    // Get rid of the almighty everlasting soft keyboard until explicitly called for
    LinearLayout rootContainer = (LinearLayout)mActivity.findViewById(R.id.rootContainer);
    setKeyboardHideListeners(rootContainer);
  }
  private class CustomTextWatcher implements TextWatcher {
    // Common listener for new / edit account input fields
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}
    @Override
    public void afterTextChanged(Editable s) { mGuiManager.clearErrorMessage(); }
  }
  private void setKeyboardHideListeners(View view) {
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
        setKeyboardHideListeners(innerView);
      }
    }
  }
  private void hideSoftKeyboard(Activity activity) {
    InputMethodManager imManager =
      (InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
    View focusedView = mActivity.getCurrentFocus();
    if (focusedView != null) {
      IBinder token = focusedView.getWindowToken();
      if (token != null) {
        imManager.hideSoftInputFromWindow(token, 0);
      }
    }
  }
  // Search bar EditText listeners
  @Override
  public void afterTextChanged(Editable s) {}
  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    mDataManager.getCursorAdapter().getFilter().filter(s.toString());
  }
  // ListView onItemClick listener
  @Override
  public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
    // Get the cursor containing data at the clicked row
    Cursor cursor = (Cursor) listView.getItemAtPosition(position);
    // Show edit screen
    mGuiManager.editAction(cursor);
  }
  // New/Edit account button onClick listeners
  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.addAccountButton:
        mGuiManager.addAction();
        break;
      case R.id.saveButton:
        mGuiManager.saveAction();
        break;
      case R.id.cancelButton:
        // Cancel
        mGuiManager.cancelAction();
        break;
      case R.id.deleteButton:
        // Delete
        mGuiManager.deleteAction();
        break;
      default:
        break;
    }
  }
}
