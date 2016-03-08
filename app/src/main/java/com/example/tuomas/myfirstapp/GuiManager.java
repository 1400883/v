package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class GuiManager {
  private final Button guiBlockerButtons[];
  private final ImageButton addAccountButton;
  private final EditText searchField;
  private final LinearLayout addEditAccountContainer;
  private final TextView addEditAccountTitle;
  private final EditText nameInputField;
  private final EditText ibanInputField;
  private final Button saveButton;
  private final Button cancelButton;
  private final Button deleteButton;
  private final ListView listview;
  private final FrameLayout listviewContainer;

  private static Activity mActivity;
  private DataManager mDataManager;
  private EventManager mEventManager;
  private static GuiManager instance = null;

  ///////////////////////////////////////////////
  // Singleton setup. Reference to activity and other managers
  // must be set before requesting instance via singleton
  ///////////////////////////////////////////////
  public static void setActivity(Activity activity) {
    mActivity = activity;
  }
  public void setManagers(EventManager eventManager, DataManager dataManager) {
    mDataManager = dataManager;
    mEventManager = eventManager;
  }

  public static GuiManager get() {
    if (instance == null) {
      instance = new GuiManager();
    }
    return instance;
  }

  ///////////////////////////////////////////////
  // Constructor
  ///////////////////////////////////////////////
  private GuiManager() {
    // Blockers
    guiBlockerButtons = new Button[2];
    guiBlockerButtons[0] = (Button)mActivity.findViewById(R.id.searchBarBlockerButton);
    guiBlockerButtons[1] = (Button)mActivity.findViewById(R.id.listviewBlockerButton);

    // Search bar area views
    addAccountButton = (ImageButton)mActivity.findViewById(R.id.addAccountButton);
    searchField = (EditText)mActivity.findViewById(R.id.searchField);

    // Listview
    listviewContainer = (FrameLayout)mActivity.findViewById(R.id.listViewContainer);
    listview = (ListView)mActivity.findViewById(R.id.listview);

    // Add&edit account overlay container
    addEditAccountContainer =
      (LinearLayout)mActivity.findViewById(R.id.addEditAccountContainer);

    // Add&edit account container contents
    addEditAccountTitle = (TextView)mActivity.findViewById(R.id.addEditAccountTitle);
    nameInputField = (EditText)mActivity.findViewById(R.id.nameInputField);
    ibanInputField = (EditText)mActivity.findViewById(R.id.ibanInputField);
    saveButton = (Button)mActivity.findViewById(R.id.saveButton);
    cancelButton = (Button)mActivity.findViewById(R.id.cancelButton);
    deleteButton = (Button)mActivity.findViewById(R.id.deleteButton);

    setListviewFrameLayoutParameters(mActivity);
  }

  ///////////////////////////////////////////////

  private void setListviewFrameLayoutParameters(Activity activity) {
    // For whatever reason, FrameLayout won't respect XML layout
    // MATCH_PARENT parameters, but insist on WRAP_CONTENT.
    // Setting them here fixes it.
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.MATCH_PARENT);

    listviewContainer.setLayoutParams(params);
  }
}
