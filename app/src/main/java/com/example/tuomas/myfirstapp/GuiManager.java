package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.database.Cursor;
import android.os.Build;
import android.text.InputFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GuiManager {
  private Button guiBlockerButtons[];
  private Button newAccountButton;
  private EditText searchField;
  private LinearLayout newEditAccountContainer;
  private LinearLayout newEditAccountInputScreen;
  private LinearLayout newEditAccountConfirmationScreen;
  private TextView newEditAccountTitle;
  private EditText nameInputField;
  private EditText ibanInputField;
  private TextView nameIbanErrorMessage;
  private TextView newEditAccountConfirmationTitle;
  private TextView newEditAccountDuplicateOldTitle;
  private TextView newEditAccountDuplicateNewTitle;
  private TextView newEditAccountDuplicateOldIban;
  private TextView newEditAccountDuplicateNewIban;
  private TextView newEditAccountConfirmationQuestion;
  private Button saveButton;
  private Button cancelButton;
  private Button deleteButton;
  private TextView toastView;
  private Toast toast;
  private ListView listview;
  private FrameLayout listviewContainer;

  private String TAG = "GuiManager";
  private GuiState mGuiState = GuiState.Main;
  private GuiState mPreviousGuiState = GuiState.Main;
  private Activity mActivity = null;
  private DataManager mDataManager;
  private EventManager mEventManager;
  private static GuiManager instance = null;

  private enum GuiState { Main, New, Edit, Delete, Confirm }
  private enum OperationResult { Success, Failure }
  private enum NewDisplayState { Show, Hide };
  private enum RequestedScreen { New, Edit, Delete, Confirm}
  private enum ErrorSource { Name, Iban, Both }
  ///////////////////////////////////////////////
  // Singleton setup. Reference to activity and other managers
  // must be set before requesting instance via singleton
  ///////////////////////////////////////////////
  public void setActivity(Activity activity) {
    if (mActivity != activity) {
      mActivity = activity;
      updateViews();
    }
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
  private GuiManager() {}
  ///////////////////////////////////////////////
  private void updateViews() {
    // Blockers
    guiBlockerButtons = new Button[2];
    guiBlockerButtons[0] = (Button)mActivity.findViewById(R.id.searchBarBlockerButton);
    guiBlockerButtons[1] = (Button)mActivity.findViewById(R.id.listviewBlockerButton);

    // Search bar area views
    newAccountButton = (Button)mActivity.findViewById(R.id.addAccountButton);
    searchField = (EditText)mActivity.findViewById(R.id.searchField);

    // Listview
    listviewContainer = (FrameLayout)mActivity.findViewById(R.id.listViewContainer);
    listview = (ListView)mActivity.findViewById(R.id.listview);

    // New / edit account overlay container
    ////////////////////////////////
    newEditAccountContainer =
      (LinearLayout)mActivity.findViewById(R.id.newEditAccountContainer);

    // New / edit account input screen
    ////////////////////////////////
    newEditAccountInputScreen =
      (LinearLayout)mActivity.findViewById(R.id.newEditAccountInputScreen);

    // Title
    newEditAccountTitle =
      (TextView)mActivity.findViewById(R.id.newEditAccountTitle);

    // Owner name and IBAN inputs
    nameInputField = (EditText)mActivity.findViewById(R.id.nameInputField);
    nameInputField.setFilters(new InputFilter[] {
      new InputFilter.LengthFilter(DataManager.MAX_OWNER_LENGTH)
    });
    ibanInputField = (EditText)mActivity.findViewById(R.id.ibanInputField);
    nameIbanErrorMessage = (TextView)mActivity.findViewById(R.id.nameIbanErrorMessage);

    // New / edit account duplicate confirmation screen
    ////////////////////////////////
    newEditAccountConfirmationScreen =
      (LinearLayout)mActivity.findViewById(R.id.newEditAccountConfirmationScreen);

    // Title
    newEditAccountConfirmationTitle =
      (TextView)mActivity.findViewById(R.id.newEditAccountConfirmationTitle);

    // Old and new IBANs
    newEditAccountDuplicateOldTitle =
      (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateOldTitle);
    newEditAccountDuplicateNewTitle =
      (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateNewTitle);
    newEditAccountDuplicateOldIban =
      (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateOldIban);
    newEditAccountDuplicateNewIban =
      (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateNewIban);

    // Confirmation question text
    newEditAccountConfirmationQuestion =
      (TextView)mActivity.findViewById(R.id.newEditAccountConfirmationQuestion);

    // Button row
    saveButton = (Button)mActivity.findViewById(R.id.saveButton);
    cancelButton = (Button)mActivity.findViewById(R.id.cancelButton);
    deleteButton = (Button)mActivity.findViewById(R.id.deleteButton);

    // Setup toast
    View toastLayout = mActivity.getLayoutInflater().inflate(
      R.layout.custom_toast,
      (ViewGroup)mActivity.findViewById(R.id.toastRoot));

    toast = new Toast(mActivity.getApplicationContext());
    toast.setView(toastLayout);
    toast.setGravity(Gravity.CENTER, 0, 0);
    toast.setDuration(Toast.LENGTH_SHORT);
    toastView = (TextView)toastLayout.findViewById(R.id.newEditAccountToast);

    setListviewFrameLayoutParameters(mActivity);
  }
  private void setListviewFrameLayoutParameters(Activity activity) {
    // For whatever reason, FrameLayout won't respect XML layout
    // MATCH_PARENT parameters, but insist on WRAP_CONTENT.
    // Setting them here fixes it.
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.MATCH_PARENT);

    listviewContainer.setLayoutParams(params);
  }
  ///////////////////////////////////////////////
  public void addAction() {
    // Show Add new account screen
    //if (newEditAccountContainer.getVisibility() == View.INVISIBLE) {
    if (mGuiState == GuiState.Main) {
      updateScreen(NewDisplayState.Show, RequestedScreen.New, "", "");
      mPreviousGuiState = GuiState.Main;
      mGuiState = GuiState.New;
    }
    else {
      Log.d(TAG,
        "addAction() fired unexpectedly, mGuiState == " + mGuiState.toString());
    }
  }
  public void cancelAction() {
    //if (newEditAccountInputScreen.getVisibility() == View.VISIBLE) {
    if (mGuiState == GuiState.New || mGuiState == GuiState.Edit) {
      // Add / edit account screen being displayed
      updateScreen(NewDisplayState.Hide, null);
      mPreviousGuiState = mGuiState;
      mGuiState = GuiState.Main;
    }
    else {
      // Duplicate owner name confirmation screen being displayed
      // -> return back to previous screen (Add account / Edit account)
      updateScreen(
        NewDisplayState.Show,
        mPreviousGuiState == GuiState.New
          ? RequestedScreen.New
          // mPreviousGuiState == GuiState.Edit implied
          : RequestedScreen.Edit);
      mGuiState = mPreviousGuiState;
      mPreviousGuiState = GuiState.Confirm;
    }
  }
  public void editAction(Cursor cursor) {
    //if (newEditAccountContainer.getVisibility() == View.INVISIBLE) {
    if (mGuiState == GuiState.Main) {
      // Get name and IBAN from the clicked listview item
      int iNameColumn = cursor.getColumnIndex(DatabaseAdapter.COLUMN_OWNER);
      int iIbanColumn = cursor.getColumnIndex(DatabaseAdapter.COLUMN_IBAN);
      String name = cursor.getString(iNameColumn);
      // Store the given name.
      // Should the user later choose to overwrite the record
      // with the same owner name, auto-assume it's a wanted
      // operation and skip asking for confirmation.
      mDataManager.setOriginalEditName(name);
      // Show IBAN as a consecutive line of characters, easier
      // for the user to copy to clipboard (should the need be)
      String iban = mDataManager.removeWhitespaces(cursor.getString(iIbanColumn));

      // Prepare and display Edit account screen
      updateScreen(
        NewDisplayState.Show, RequestedScreen.Edit, name, iban);
      mPreviousGuiState = mGuiState;
      mGuiState = GuiState.Edit;
    }
    else {
      Log.d(TAG,
        "editAction() fired unexpectedly, mGuiState == " + mGuiState.toString());
    }
  }
  public void deleteAction() {
    // Prepare and display Delete account screen
    String originalName = mDataManager.getOriginalEditName();
    String originalIban = mDataManager.getIbanFromName(originalName);
    originalIban = mDataManager.formatIban(originalIban);
    if (originalIban.equals("")) {
      Log.d(TAG, "Owner '" + originalName + "' did not match any account.");
      originalIban = mActivity.getString(R.string.internal_error);
    }
    String postTitle = mActivity.getString(R.string.deleteAccount_confirmationTitle);
    updateScreen(
      NewDisplayState.Show, RequestedScreen.Delete, originalName, postTitle, originalIban);
    mPreviousGuiState = mGuiState;
    mGuiState = GuiState.Delete;
    // String originalName = mDataManager.getOriginalEditName();
    // deleteAccount(originalName);
    // int id = mDataManager.getIdFromName(originalName);
  }
  public void saveAction() {
    String name = nameInputField.getText().toString();
    String iban = ibanInputField.getText().toString();
    // Apply nice visual formatting, which will be
    // the format IBAN will be in in the database
    iban = mDataManager.formatIban(iban);

    //if (newEditAccountInputScreen.getVisibility() == View.VISIBLE) {
    if (mGuiState == GuiState.New || mGuiState == GuiState.Edit) {
      // Add / edit account screen being displayed
      boolean isNameValid = DataManager.InputValidator.isValidName(name);
      boolean isIbanValid = DataManager.InputValidator.isValidIban(iban);

      if (isNameValid) {
        setErrorMessageState(NewDisplayState.Hide, ErrorSource.Name);
        if (isIbanValid) {
          // Both valid
          // NOTE: Using case-sensitive name comparison
          if (mDataManager.isUniqueName(name)) {
            if (mGuiState == GuiState.New) {
              // New name unique in the database -> create account
              createAccount(name, iban);
            }
            else {
              // Name changed to unique via editing an existing record
              // -> do replacement
              String originalName = mDataManager.getOriginalEditName();
              int originalId = mDataManager.getIdFromName(originalName);
              replaceAccount(originalId, name, iban);
            }
          }
          else if (mDataManager.isSameName(name) && mGuiState == GuiState.Edit) {
            // Name not unique but remained the same after editing
            // -> replace without confirmation. The original record
            // will be overwritten, so no need to delete anything.
            replaceAccount(mDataManager.getIdFromName(name), name, iban);
          }
          else {
            // Duplicate other name found -> populate and show confirmation screen

            // Get correct title message part
            String postTitle = mGuiState == GuiState.New
              ? mActivity.getString(R.string.newAccount_duplicateTitle)
              : mActivity.getString(R.string.editAccount_duplicateTitle);

            String oldIban = mDataManager.getIbanFromName(name);
            if (oldIban.equals("")) {
              Log.w(TAG, "Owner '" + name + "' did not match any account.");
              oldIban = mActivity.getString(R.string.internal_error);
            }

            // Show confirmation screen
            updateScreen(NewDisplayState.Show, RequestedScreen.Confirm,
              name, postTitle, oldIban, iban);
            mPreviousGuiState = mGuiState;
            mGuiState = GuiState.Confirm;
          }
        }
        else {
          // Invalid IBAN
          setErrorMessageState(NewDisplayState.Show, ErrorSource.Iban);
        }
      }
      else { // isNameValid() == false
        if (isIbanValid) {
          // Invalid owner name
          setErrorMessageState(NewDisplayState.Show, ErrorSource.Name);
        }
        else {
          // Owner name and IBAN both invalid
          setErrorMessageState(NewDisplayState.Show, ErrorSource.Both);
        }
      }
    }
    else if (mGuiState == GuiState.Confirm){
      // Duplicate entry confirmation screen being displayed
      int idToReplaceTo = mDataManager.getIdFromName(name);
      // String originalId = mDataManager.getIdFromName()
      // new -> replace by name
      // edit -> replace by name, delete by original name's id
      if (mDataManager.replaceAccount(idToReplaceTo, name, iban)) {
        //if (!isNewAccountOrigin()) {
        if (mPreviousGuiState == GuiState.Edit) {
          // Replacement was done via Edit account feature, so another
          // record was replaced with the edited data. The original
          // unmodified record is still present in the database and
          // needs to be deleted.
          // NOTE: Kinda sloppy to use owner names as identifiers,
          // but they _should_ be unique in the database at all times,
          // by the app design. This code should totally be modified
          // to store and use ids always if available, though.
          String originalName = mDataManager.getOriginalEditName();
          //int idToReplaceFrom = mDataManager.getIdFromName(originalName);
          if (mDataManager.deleteAccount(originalName)) {
            mDataManager.refreshAccountData();
          }
          else {
            Log.d(TAG, "D'ooh! Failed account deletion after replacement.");
          }
        }
        displayToastMessage(
          OperationResult.Success, R.string.updateAccount_success);
      }
      else {
        displayToastMessage(
          OperationResult.Failure, R.string.updateAccount_failure);
      }
      updateScreen(NewDisplayState.Hide, null);
      mPreviousGuiState = mGuiState;
      mGuiState = GuiState.Main;
    }
    else { // mGuiState == GuiState.
      String originalName = mDataManager.getOriginalEditName();
      deleteAccount(originalName);
    }
  }
  ///////////////////////////////////////////////
  private void createAccount(String name, String iban) {
    if (mDataManager.createAccount(name, iban)) {
      doAccountOperationAftermath(R.string.newAccount_success);
    }
    else {
      displayToastMessage(OperationResult.Failure, R.string.newAccount_failure);
    }
  }
  private void replaceAccount(int id, String name, String iban) {
    if (mDataManager.replaceAccount(id, name, iban)) {
      doAccountOperationAftermath(R.string.updateAccount_success);
    }
    else {
      displayToastMessage(OperationResult.Failure, R.string.updateAccount_failure);
    }
  }
  private void deleteAccount(String name) {
    if (mDataManager.deleteAccount(name)) {
      doAccountOperationAftermath(R.string.deleteAccount_success);
    }
    else {
      displayToastMessage(OperationResult.Failure, R.string.deleteAccount_failure);
    }
  }
  private void doAccountOperationAftermath(int messageId) {
    mDataManager.refreshAccountData();
    displayToastMessage(OperationResult.Success, messageId);
    updateScreen(NewDisplayState.Hide, null);
    mPreviousGuiState = mGuiState;
    mGuiState = GuiState.Main;
  }
  private void displayToastMessage(OperationResult type, int messageId) {
    toastView.setText(messageId);

    // NOTE: getColor(int) was deprecated in API level 23.
    // Since API level 23, the signature has been getColor(int, Resources.Theme).
    // Find OS version API level and get color using the right sig.
    int colorResource = type == OperationResult.Success
      ? R.color.toast_successTextColor
      : R.color.toast_errorTextColor;
    int color =
      Build.VERSION.SDK_INT < 23
        ? mActivity.getResources().getColor(colorResource)
        : mActivity.getResources().getColor(colorResource, null);
    toastView.setTextColor(color);
    toast.show();
  }
  /////////////////////////////////////////////
  public void clearErrorMessage() { setErrorMessageState(NewDisplayState.Hide, null); }
  private void setErrorMessageState(NewDisplayState state, ErrorSource source) {
    // NOTE: if state == NewDisplayState.Hide, source may be null
    if (state == NewDisplayState.Show) {
      String errorString =
        source == ErrorSource.Name
          ? mActivity.getString(R.string.newEditAccount_nameError)
        : source == ErrorSource.Iban
          ? mActivity.getString(R.string.newEditAccount_ibanError)
          // source == ErrorSource.Both implied
        : mActivity.getString(R.string.newEditAccount_nameError) + " " +
          mActivity.getString(R.string.newEditAccount_ibanError);
      nameIbanErrorMessage.setText(errorString);
      nameIbanErrorMessage.setVisibility(View.VISIBLE);
      return;
    }
    nameIbanErrorMessage.setVisibility(View.INVISIBLE);
  }
  /////////////////////////////////////////////
  private void updateScreen(NewDisplayState state,
                            RequestedScreen content,
                            String... additionalData) {
    // NOTE: Keep previous name and input field contents by not passing additionalData
    // NOTE: if state == NewDisplayState.Hide, RequestedScreen may be null (not used)

    // Show / hide blocker buttons
    setBlockerButtonDisplayState(state);

    if (state == NewDisplayState.Show) {
      switch (content) {
        case New:
        case Edit:
          // additionalData[]: name, iban OR absent (empty array)

          // Set main title
          newEditAccountTitle.setText(content == RequestedScreen.New
            ? R.string.newAccount_title
            : R.string.editAccount_title);

          // Set error messages invisible
          setErrorMessageState(NewDisplayState.Hide, ErrorSource.Both);

          // Init input fields and set focus to name field
          if (additionalData.length > 0) {
            String name = additionalData[0];
            String iban = additionalData[1];
            nameInputField.setText(name);
            ibanInputField.setText(iban);
          }
          nameInputField.requestFocus();

          // Button texts
          saveButton.setText(R.string.newEditAccount_saveButton);
          cancelButton.setText(R.string.newEditAccount_cancelButton);

          // Show / hide delete button
          deleteButton.setVisibility(content == RequestedScreen.New
            ? View.GONE
            : View.VISIBLE);

          // Hide confirmation layout container
          newEditAccountConfirmationScreen.setVisibility(View.INVISIBLE);
          // Show add / edit input layout container
          newEditAccountInputScreen.setVisibility(View.VISIBLE);
          break;
        case Delete:
        case Confirm:
          // additionalData[]: name, @string/[new/edit]Account_duplicateTitle, oldIban, newIban
          // additionalData[]: name, @string/deleteAccount_confirmationTitle, iban

          // Set main title
          newEditAccountTitle.setText(content == RequestedScreen.Delete
            ? R.string.deleteAccount_title
            : R.string.editAccount_title);

          // Set confirmation title
          newEditAccountConfirmationTitle.setText(
            "\"" + additionalData[0] + "\"" + additionalData[1]);

          // Set old and new IBANs
          ///////////////////////////////////

          // Show / Hide old IBAN title
          newEditAccountDuplicateOldTitle.setVisibility(content == RequestedScreen.Delete
            ? View.INVISIBLE : View.VISIBLE);

          // Replace new IBAN title
          newEditAccountDuplicateNewTitle.setText(content == RequestedScreen.Delete
            ? R.string.newEditAccount_deleteIbanTitle
            : R.string.newEditAccount_duplicateNewIbanTitle);

          // Show / hide old IBAN
          newEditAccountDuplicateOldIban.setVisibility(content == RequestedScreen.Delete
            ? View.GONE : View.VISIBLE);

          // Replace confirmation question
          newEditAccountConfirmationQuestion.setText(content == RequestedScreen.Delete
            ? R.string.newEditAccount_deleteConfirmation
            : R.string.newEditAccount_duplicateConfirmation);

          if (content == RequestedScreen.Delete) {
            // Replace new IBAN
            newEditAccountDuplicateNewIban.setText(additionalData[2]);
          }
          else {
            // Replace old and new IBANs
            newEditAccountDuplicateOldIban.setText(additionalData[2]);
            newEditAccountDuplicateNewIban.setText(additionalData[3]);
          }

          // Set button texts
          saveButton.setText(R.string.newEditAccount_yesButton);
          cancelButton.setText(R.string.newEditAccount_noButton);

          // Hide delete button
          deleteButton.setVisibility(View.GONE);

          // Hide add / edit input layout container
          newEditAccountInputScreen.setVisibility(View.INVISIBLE);
          // Show confirmation layout container
          newEditAccountConfirmationScreen.setVisibility(View.VISIBLE);
          break;
      }
      // Show screen layout container
      newEditAccountContainer.setVisibility(View.VISIBLE);
    }
    else { // state == NewDisplayState.Hide
      // Hide screen layout container
      newEditAccountContainer.setVisibility(View.INVISIBLE);
    }
  }
  private void setBlockerButtonDisplayState(NewDisplayState state) {
    for (Button button : guiBlockerButtons) {
      button.setVisibility(state == NewDisplayState.Show
        ? View.VISIBLE
        : View.INVISIBLE);
    }
  }
  /////////////////////////////////////////////
}