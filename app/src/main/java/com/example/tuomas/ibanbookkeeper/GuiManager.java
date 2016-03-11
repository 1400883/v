package com.example.tuomas.ibanbookkeeper;

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
  private String TAG = "GuiManager";

  // GUI view groups and views
  private Button mGuiBlockerButtons[];
  private Button mNewAccountButton;
  private EditText mSearchField;
  private LinearLayout mNewEditAccountContainer;
  private LinearLayout mNewEditAccountInputScreen;
  private LinearLayout mNewEditAccountConfirmationScreen;
  private TextView mNewEditAccountTitle;
  private EditText mNameInputField;
  private EditText mIbanInputField;
  private TextView mNameIbanErrorMessage;
  private TextView mNewEditAccountConfirmationTitle;
  private TextView mNewEditAccountDuplicateOldTitle;
  private TextView mNewEditAccountDuplicateNewTitle;
  private TextView mNewEditAccountDuplicateOldIban;
  private TextView mNewEditAccountDuplicateNewIban;
  private TextView mNewEditAccountConfirmationQuestion;
  private Button mSaveButton;
  private Button mCancelButton;
  private Button mDeleteButton;
  private TextView mToastView;
  private Toast mToast;
  private ListView mListview;
  private FrameLayout mListviewContainer;

  // Maintain GUI states to keep track of location in the tree
  private GuiState mGuiState = GuiState.Main;
  private GuiState mPreviousGuiState = GuiState.Main;

  private Activity mActivity = null;
  private DataManager mDataManager;
  private EventManager mEventManager;
  private static GuiManager instance = null;

  public enum GuiState { Main, New, Edit, Delete, Confirm }
  public enum NewDisplayState { Show, Hide };
  public enum RequestedScreen { New, Edit, Delete, Confirm}
  private enum OperationResult { Success, Failure }
  private enum ErrorSource { Name, Iban, Both }
  ///////////////////////////////////////////////
  // Singleton
  ///////////////////////////////////////////////
  public static GuiManager get() {
    if (instance == null) {
      instance = new GuiManager();
    }
    return instance;
  }
  private GuiManager() {}
  public void updateReferences(Activity activity) {
    if (mActivity != activity) {
      // Update activity references on activity restart.
      // Should be no mem leaks here...
      mActivity = activity;
      if (mDataManager == null || mEventManager == null) {
        mDataManager = DataManager.get();
        mEventManager = EventManager.get();
      }
      updateViews();
    }
  }
  ///////////////////////////////////////////////
  // GUI view reference setup
  ///////////////////////////////////////////////
  private void updateViews() {
    // Blockers
    mGuiBlockerButtons = new Button[2];
    mGuiBlockerButtons[0] = (Button)mActivity.findViewById(R.id.searchBarBlockerButton);
    mGuiBlockerButtons[1] = (Button)mActivity.findViewById(R.id.listviewBlockerButton);

    // Search bar area views
    mNewAccountButton = (Button)mActivity.findViewById(R.id.addAccountButton);
    mSearchField = (EditText)mActivity.findViewById(R.id.searchField);

    // Result set listview
    mListviewContainer = (FrameLayout)mActivity.findViewById(R.id.listViewContainer);
    mListview = (ListView)mActivity.findViewById(R.id.listview);

    ///////////////////////////////////////////////
    // 1 New / edit account overlay container
    ///////////////////////////////////////////////
    mNewEditAccountContainer =
      (LinearLayout)mActivity.findViewById(R.id.newEditAccountContainer);

    ///////////////////////////////////////////////
    // 1.1 New / edit account input screen
    ///////////////////////////////////////////////
    mNewEditAccountInputScreen =
      (LinearLayout)mActivity.findViewById(R.id.newEditAccountInputScreen);

    // Title
    mNewEditAccountTitle =
      (TextView)mActivity.findViewById(R.id.newEditAccountTitle);

    // Owner name and IBAN inputs
    mNameInputField = (EditText)mActivity.findViewById(R.id.nameInputField);
    // Limit length
    mNameInputField.setFilters(new InputFilter[] {
      new InputFilter.LengthFilter(DataManager.MAX_OWNER_LENGTH)
    });
    mIbanInputField = (EditText)mActivity.findViewById(R.id.ibanInputField);

    // Validation error message
    mNameIbanErrorMessage = (TextView)mActivity.findViewById(R.id.nameIbanErrorMessage);

    ///////////////////////////////////////////////
    // 1.2 New / edit account duplicate confirmation screen
    ///////////////////////////////////////////////
    mNewEditAccountConfirmationScreen =
      (LinearLayout)mActivity.findViewById(R.id.newEditAccountConfirmationScreen);

    // Title
    mNewEditAccountConfirmationTitle =
      (TextView)mActivity.findViewById(R.id.newEditAccountConfirmationTitle);

    // Old and new IBANs
    mNewEditAccountDuplicateOldTitle =
      (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateOldTitle);
    mNewEditAccountDuplicateNewTitle =
      (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateNewTitle);
    mNewEditAccountDuplicateOldIban =
      (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateOldIban);
    mNewEditAccountDuplicateNewIban =
      (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateNewIban);

    // Confirmation question text
    mNewEditAccountConfirmationQuestion =
      (TextView)mActivity.findViewById(R.id.newEditAccountConfirmationQuestion);

    // Button row
    mSaveButton = (Button)mActivity.findViewById(R.id.saveButton);
    mCancelButton = (Button)mActivity.findViewById(R.id.cancelButton);
    mDeleteButton = (Button)mActivity.findViewById(R.id.deleteButton);

    ///////////////////////////////////////////////
    // Setup toast
    ///////////////////////////////////////////////
    View toastLayout = mActivity.getLayoutInflater().inflate(
      R.layout.custom_toast,
      (ViewGroup)mActivity.findViewById(R.id.toastRoot));

    mToast = new Toast(mActivity.getApplicationContext());
    mToast.setView(toastLayout);
    mToast.setGravity(Gravity.CENTER, 0, 0);
    mToast.setDuration(Toast.LENGTH_SHORT);
    mToastView = (TextView)toastLayout.findViewById(R.id.newEditAccountToast);

    setListviewFrameLayoutParameters(mActivity);
  }
  private void setListviewFrameLayoutParameters(Activity activity) {
    // For whatever reason, FrameLayout won't respect XML layout
    // MATCH_PARENT parameters, but insist on WRAP_CONTENT.
    // Setting them on runtime fixes it.
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,
      LinearLayout.LayoutParams.MATCH_PARENT);
    mListviewContainer.setLayoutParams(params);
  }
  ///////////////////////////////////////////////
  // GUI state getters
  ///////////////////////////////////////////////
  public GuiState getGuiState() { return mGuiState; }
  public GuiState getPreviousGuiState() { return mPreviousGuiState; }
  ///////////////////////////////////////////////
  // GUI tree traversal management
  // Add, Save and Cancel are accessed via GUI buttons
  // Edit is accessed by touching listview item in the main screen
  ///////////////////////////////////////////////
  public void addAction() {
    if (mGuiState == GuiState.Main) {
      // Display Add new account screen
      updateScreen(NewDisplayState.Show, RequestedScreen.New, "", "");
      mPreviousGuiState = mGuiState;
      mGuiState = GuiState.New;
    }
    else {
      Log.d(TAG,
        "addAction() fired unexpectedly, mGuiState == " + mGuiState.toString());
    }
  }
  public void cancelAction() {
    if (mGuiState == GuiState.New || mGuiState == GuiState.Edit) {
      // Add / edit account screen being displayed
      updateScreen(NewDisplayState.Hide, null);
      mPreviousGuiState = mGuiState;
      mGuiState = GuiState.Main;
    }
    else { // mGuiState == GuiState.Confirm || mGuiState == GuiState.Delete implied
      // Duplicate owner name confirmation screen being displayed
      // -> return back to previous screen (Add account / Edit account)
      updateScreen(
        NewDisplayState.Show,
        mPreviousGuiState == GuiState.New
          ? RequestedScreen.New
          // mPreviousGuiState == GuiState.Edit implied
          : RequestedScreen.Edit);
      GuiState currentGuiState = mGuiState;
      mGuiState = mPreviousGuiState;
      mPreviousGuiState = currentGuiState; // GuiState.Confirm or GuiState.Delete
    }
  }
  public void editAction(Cursor cursor) {
    if (mGuiState == GuiState.Main) {
      // Get name and IBAN from the clicked mListview item
      int iNameColumn = cursor.getColumnIndex(DatabaseAdapter.COLUMN_OWNER);
      int iIbanColumn = cursor.getColumnIndex(DatabaseAdapter.COLUMN_IBAN);
      String name = cursor.getString(iNameColumn);
      // Store the given name at this point into session data storage.
      // Should the user later choose to overwrite the record
      // with the same owner name, auto-assume it's a wanted
      // operation and skip asking for confirmation.
      // This is also used in delete functionality, when the
      // user may have modified account owner name and/or IBAN
      // fields in edit screen before pressing Del. This should
      // lead to deletion of the original entry, not one (if any)
      // that matches modified values.
      mDataManager.setOriginalName(name);
      // Show IBAN as a consecutive line of characters, easier
      // for the user to copy it to clipboard (should the need be)
      String iban = mDataManager.removeWhitespaces(cursor.getString(iIbanColumn));

      // Display Edit account screen
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
    // Delete account action can only be reached through Edit account
    // screen, where the user may have edited either or both of input
    // fields prior to pressing del. Target delete action to the original
    // entry, not modified values (even if they match something in the
    // database).
    String originalName = mDataManager.getOriginalName();
    String originalIban = mDataManager.getIbanFromName(originalName);
    originalIban = mDataManager.formatIban(originalIban);
    if (originalIban.equals("")) {
      Log.d(TAG, "Owner '" + originalName + "' did not match any account.");
      originalIban = mActivity.getString(R.string.internal_error);
    }
    String postTitle = mActivity.getString(R.string.deleteAccount_confirmationTitle);
    // Display Delete account screen
    updateScreen(
      NewDisplayState.Show,
      RequestedScreen.Delete,
      originalName,
      postTitle,
      originalIban);
    mPreviousGuiState = mGuiState;
    mGuiState = GuiState.Delete;
  }
  public void saveAction() {
    String name = mNameInputField.getText().toString();
    String iban = mIbanInputField.getText().toString();
    // Apply nice visual grouping formatting, which is
    // the format it will be stored in in the database
    iban = mDataManager.formatIban(iban);

    if (mGuiState == GuiState.New || mGuiState == GuiState.Edit) {
      // Add / edit account screen being displayed
      boolean isValidName = DataManager.InputValidator.isValidName(name);
      boolean isValidIban = DataManager.InputValidator.isValidIban(iban);

      if (isValidName) {
        setErrorMessageState(NewDisplayState.Hide, ErrorSource.Name);
        if (isValidIban) {
          // Both name and IBAN valid
          // NOTE: Using case-sensitive name uniqueness comparison
          if (mDataManager.isUniqueName(name)) {
            if (mGuiState == GuiState.New) {
              // New name unique in the database -> create account
              createAccount(name, iban);
            }
            else { // mGuiState == GuiState.Edit implied
              // Name changed to unique via editing an existing record
              // -> do replacement
              String originalName = mDataManager.getOriginalName();
              int originalId = mDataManager.getIdFromName(originalName);
              replaceAccount(originalId, name, iban);
            }
          }
          else if (mGuiState == GuiState.Edit && mDataManager.isSameAsOriginalName(name)) {
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
            updateScreen(
              NewDisplayState.Show,
              RequestedScreen.Confirm,
              name,
              postTitle,
              oldIban,
              iban);
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
        if (isValidIban) {
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
      if (mDataManager.replaceAccount(idToReplaceTo, name, iban)) {
        if (mPreviousGuiState == GuiState.Edit) {
          // Replacement was done via Edit account feature, so another
          // record was replaced with the edited data. The original
          // unmodified record is still present in the database and
          // needs to be deleted.
          // NOTE: Kinda sloppy to use owner names as identifiers,
          // but they _should_ be unique in the database at all times,
          // by the app design. This code should totally be modified
          // to store and use ids always if available, though.
          String originalName = mDataManager.getOriginalName();
          //int idToReplaceFrom = mDataManager.getIdFromName(originalName);
          if (!mDataManager.deleteAccount(originalName)) {
            Log.d(TAG, "D'ooh! Failed account deletion after replacement.");
          }
        }
        mDataManager.updateResultsToScreen();
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
    else { // mGuiState == GuiState.Delete
      String originalName = mDataManager.getOriginalName();
      deleteAccount(originalName);
    }
  }
  ///////////////////////////////////////////////
  // Database command redirection & result message display
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
    mDataManager.updateResultsToScreen();
    displayToastMessage(OperationResult.Success, messageId);
    updateScreen(NewDisplayState.Hide, null);
    mPreviousGuiState = mGuiState;
    mGuiState = GuiState.Main;
  }
  private void displayToastMessage(OperationResult type, int messageId) {
    mToastView.setText(messageId);

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
    mToastView.setTextColor(color);
    mToast.show();
  }
  ///////////////////////////////////////////////
  // Error message handling
  ///////////////////////////////////////////////
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
      mNameIbanErrorMessage.setText(errorString);
      mNameIbanErrorMessage.setVisibility(View.VISIBLE);
      return;
    }
    mNameIbanErrorMessage.setVisibility(View.INVISIBLE);
  }
  ///////////////////////////////////////////////
  // Screen content updating based on current location in tree
  ///////////////////////////////////////////////
  public void updateScreen(NewDisplayState state,
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
          mNewEditAccountTitle.setText(content == RequestedScreen.New
            ? R.string.newAccount_title
            : R.string.editAccount_title);

          // Set error messages invisible
          setErrorMessageState(NewDisplayState.Hide, ErrorSource.Both);

          // Init input fields and set focus to name field
          if (additionalData.length > 0) {
            String name = additionalData[0];
            String iban = additionalData[1];
            mNameInputField.setText(name);
            mIbanInputField.setText(iban);
          }
          mNameInputField.requestFocus();

          // Button texts
          mSaveButton.setText(R.string.newEditAccount_saveButton);
          mCancelButton.setText(R.string.newEditAccount_cancelButton);

          // Show / hide delete button
          mDeleteButton.setVisibility(content == RequestedScreen.New
            ? View.GONE
            : View.VISIBLE);

          // Hide confirmation layout container
          mNewEditAccountConfirmationScreen.setVisibility(View.INVISIBLE);
          // Show add / edit input layout container
          mNewEditAccountInputScreen.setVisibility(View.VISIBLE);
          break;
        case Delete:
        case Confirm:
          // additionalData[]: name, @string/[new/edit]Account_duplicateTitle, oldIban, newIban
          // additionalData[]: name, @string/deleteAccount_confirmationTitle, iban

          // Set main title
          mNewEditAccountTitle.setText(content == RequestedScreen.Delete
            ? R.string.deleteAccount_title
            : R.string.editAccount_title);

          // Set confirmation title
          mNewEditAccountConfirmationTitle.setText(
            "\"" + additionalData[0] + "\"" + additionalData[1]);

          // Set old and new IBANs
          ///////////////////////////////////

          // Show / Hide old IBAN title
          mNewEditAccountDuplicateOldTitle.setVisibility(content == RequestedScreen.Delete
            ? View.INVISIBLE : View.VISIBLE);

          // Replace new IBAN title
          mNewEditAccountDuplicateNewTitle.setText(content == RequestedScreen.Delete
            ? R.string.newEditAccount_deleteIbanTitle
            : R.string.newEditAccount_duplicateNewIbanTitle);

          // Show / hide old IBAN
          mNewEditAccountDuplicateOldIban.setVisibility(content == RequestedScreen.Delete
            ? View.GONE : View.VISIBLE);

          // Replace confirmation question
          mNewEditAccountConfirmationQuestion.setText(content == RequestedScreen.Delete
            ? R.string.newEditAccount_deleteConfirmation
            : R.string.newEditAccount_duplicateConfirmation);

          if (content == RequestedScreen.Delete) {
            // Replace new IBAN
            mNewEditAccountDuplicateNewIban.setText(additionalData[2]);
          }
          else {
            // Replace old and new IBANs
            mNewEditAccountDuplicateOldIban.setText(additionalData[2]);
            mNewEditAccountDuplicateNewIban.setText(additionalData[3]);
          }

          // Set button texts
          mSaveButton.setText(R.string.newEditAccount_yesButton);
          mCancelButton.setText(R.string.newEditAccount_noButton);

          // Hide delete button
          mDeleteButton.setVisibility(View.GONE);

          // Hide add / edit input layout container
          mNewEditAccountInputScreen.setVisibility(View.INVISIBLE);
          // Show confirmation layout container
          mNewEditAccountConfirmationScreen.setVisibility(View.VISIBLE);
          break;
      }
      // Show screen layout container
      mNewEditAccountContainer.setVisibility(View.VISIBLE);
    }
    else { // state == NewDisplayState.Hide
      // Hide screen layout container
      mNewEditAccountContainer.setVisibility(View.INVISIBLE);
    }
  }
  private void setBlockerButtonDisplayState(NewDisplayState state) {
    // Disabling GUI layers below the active screen area
    // is implemented using transparent buttons that cover areas
    // that need to be activated / inactivated. Accessibility is
    // controller by switching button visibility.
    for (Button button : mGuiBlockerButtons) {
      button.setVisibility(state == NewDisplayState.Show
        ? View.VISIBLE
        : View.INVISIBLE);
    }
  }
}