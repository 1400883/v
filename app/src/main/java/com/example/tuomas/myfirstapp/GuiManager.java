package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GuiManager {
  private final Button guiBlockerButtons[];
  private final Button addAccountButton;
  private final EditText searchField;
  private final LinearLayout addEditAccountContainer;
  private final TextView addEditAccountTitle;
  private final EditText nameInputField;
  private final TextView nameErrorMessage;
  private final EditText ibanInputField;
  private final TextView ibanErrorMessage;
  private final Button saveButton;
  private final Button cancelButton;
  private final Button deleteButton;
  private final ListView listview;
  private final FrameLayout listviewContainer;

  private static Activity mActivity;
  private DataManager mDataManager;
  private EventManager mEventManager;
  private static GuiManager instance = null;

  private enum DisplayState { Show, Hide };
  private enum DialogType { NewAccount, EditAccount }
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
    addAccountButton = (Button)mActivity.findViewById(R.id.addAccountButton);
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
    nameErrorMessage = (TextView)mActivity.findViewById(R.id.nameErrorMessage);
    ibanInputField = (EditText)mActivity.findViewById(R.id.ibanInputField);
    ibanErrorMessage = (TextView)mActivity.findViewById(R.id.ibanErrorMessage);
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

  public void onAddAccountButton() {
    if (addEditAccountContainer.getVisibility() != View.VISIBLE) {
      setNewEditDialogDisplayState(DisplayState.Show, DialogType.NewAccount);
    }
  }
  public void onCancelButton() {
    setNewEditDialogDisplayState(DisplayState.Hide, DialogType.NewAccount);
  }
  public void onSaveButton() {
    boolean isNameValid = false;
    boolean isIbanValid = false;
    String name = nameInputField.getText().toString();
    String iban = ibanInputField.getText().toString();
    isNameValid = mDataManager.isValidName(name);
    isIbanValid = mDataManager.isValidIban(iban);
    if (isNameValid) {
      nameErrorMessage.setVisibility(View.INVISIBLE);
      if (isIbanValid) {
        // Both valid
        ibanErrorMessage.setVisibility(View.INVISIBLE);
        // Create account
        if (mDataManager.createAccount(name, iban)) {
          mDataManager.refreshData();
          displaySuccessMessage();
          setNewEditDialogDisplayState(DisplayState.Hide, DialogType.NewAccount);
        }
      }
      else {
        ibanErrorMessage.setVisibility(View.VISIBLE);
      }
    }
    else {
      nameErrorMessage.setVisibility(View.VISIBLE);
      if (!isIbanValid) {
        ibanErrorMessage.setVisibility(View.VISIBLE);
      }
    }
  }

  private void displaySuccessMessage() {
    View toastLayout = mActivity.getLayoutInflater().inflate(
      R.layout.custom_toast,
      (ViewGroup)mActivity.findViewById(R.id.toastRoot));

    Toast toast = new Toast(mActivity.getApplicationContext());
    toast.setView(toastLayout);
    toast.setGravity(Gravity.CENTER, 0, 0);
    toast.setDuration(Toast.LENGTH_SHORT);
    toast.show();
  }

  private void setNewEditDialogDisplayState(DisplayState state, DialogType type) {
    // Show / hide blockers
    for (Button button : guiBlockerButtons) {
      button.setVisibility(state == DisplayState.Show ? View.VISIBLE : View.INVISIBLE);
    }

    if (state == DisplayState.Show) {
      // Set title
      addEditAccountTitle.setText(
        type == DialogType.NewAccount
          ? R.string.newAccount_title
          : R.string.editAccount_title);

      // Remove error messages
      nameErrorMessage.setVisibility(View.INVISIBLE);
      ibanErrorMessage.setVisibility(View.INVISIBLE);

      if (type == DialogType.NewAccount) {
        // Reset input fields and set focus to name field
        nameInputField.setText("");
        nameInputField.requestFocus();
        ibanInputField.setText("FI7053900050004503");

      }

      // Show / hide delete button
      deleteButton.setVisibility(type == DialogType.NewAccount
        ? View.GONE : View.VISIBLE);
    }

    // Show / hide container
    addEditAccountContainer.setVisibility(
      state == DisplayState.Show ? View.VISIBLE : View.INVISIBLE);
  }
}
