package com.example.tuomas.myfirstapp;

import android.app.Activity;
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
  private final Button guiBlockerButtons[];
  private final Button newAccountButton;
  private final EditText searchField;
  private final LinearLayout newEditAccountContainer;
  private final LinearLayout newEditAccountInputScreen;
  private final LinearLayout newEditAccountConfirmationScreen;
  private final TextView newEditAccountTitle;
  private final EditText nameInputField;
  private final EditText ibanInputField;
  private final TextView nameIbanErrorMessage;
  private final TextView newEditAccountDuplicateTitle;
  private final TextView newEditAccountDuplicateOldIban;
  private final TextView newEditAccountDuplicateNewIban;
  private final Button saveButton;
  private final Button cancelButton;
  private final Button deleteButton;
  private final TextView toastMessage;
  private final Toast toast;
  private final ListView listview;
  private final FrameLayout listviewContainer;

  private static Activity mActivity;
  private DataManager mDataManager;
  private EventManager mEventManager;
  private static GuiManager instance = null;

  private enum DisplayState { Show, Hide };
  private enum DialogType { NewAccount, EditAccount }
  private enum ScreenContent { Input, Confirmation }
  private enum SuccessType { New, Update }
  private enum ErrorSource { Name, Iban, Both }
  ///////////////////////////////////////////////
  // Singleton setup. Reference to activity and other managers
  // must be set before requesting instance via singleton
  ///////////////////////////////////////////////
  public static void setActivity(Activity activity) { mActivity = activity; }
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
        ibanInputField = (EditText)mActivity.findViewById(R.id.ibanInputField);
        nameIbanErrorMessage = (TextView)mActivity.findViewById(R.id.nameIbanErrorMessage);

      // New / edit account duplicate confirmation screen
      ////////////////////////////////
      newEditAccountConfirmationScreen =
        (LinearLayout)mActivity.findViewById(R.id.newEditAccountConfirmationScreen);

        // Title
        newEditAccountDuplicateTitle =
          (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateTitle);

        // Old and new IBANs
        newEditAccountDuplicateOldIban =
          (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateOldIban);
        newEditAccountDuplicateNewIban =
          (TextView)mActivity.findViewById(R.id.newEditAccountDuplicateNewIban);

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

      // Toast
      toastMessage = (TextView)toastLayout.findViewById(R.id.newEditAccountToast);

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

  public void onNewAccountButton() {
    if (newEditAccountContainer.getVisibility() != View.VISIBLE) {
      setNewEditDialogScreenContent(ScreenContent.Input);
      setNewEditDialogDisplayState(DisplayState.Show, DialogType.NewAccount);
    }
  }
  public void onCancelButton() {
    if (newEditAccountInputScreen.getVisibility() == View.VISIBLE) {
      // Add / edit account screen being displayed
      setNewEditDialogDisplayState(DisplayState.Hide, DialogType.NewAccount);
    }
    else {
      // Duplicate confirmation screen being displayed
      setNewEditDialogScreenContent(ScreenContent.Input);
    }

  }
  public void onSaveButton() {
    if (newEditAccountInputScreen.getVisibility() == View.VISIBLE) {
      // Add / edit account screen being displayed
      boolean isNameValid = false;
      boolean isIbanValid = false;
      String name = getNameInput();
      String iban = getFormattedIbanInput();

      isNameValid = DataManager.InputValidator.isValidName(name);
      isIbanValid = DataManager.InputValidator.isValidIban(iban);

      if (isNameValid) {
        setErrorMessage(ErrorSource.Name, DisplayState.Hide);
        if (isIbanValid) {
          // Both valid
          if (!mDataManager.isDuplicateName(name)) {
            // Create account
            if (mDataManager.createAccount(name, iban)) {
              mDataManager.refreshDisplay();
              displaySuccessMessage(SuccessType.New);
              setNewEditDialogDisplayState(DisplayState.Hide, DialogType.NewAccount);
            }
          }
          else {
            // Duplicate name found -> populate and show confirmation screen

            // Title
            newEditAccountDuplicateTitle
              .setText(name + mActivity.getString(R.string.newEditAccount_duplicateTitle));

            // Old IBAN found in the database
            try {
              newEditAccountDuplicateOldIban
                .setText(mDataManager.getIbanFromName(name));
            }
            catch (Exception ex) {
              // Should never end up here...
              Log.w(this.toString(), ex.getMessage());
              newEditAccountDuplicateOldIban.setText(R.string.internal_error);
            }

            // New user-submitted IBAN
            newEditAccountDuplicateNewIban.setText(iban);

            // Show screen
            setNewEditDialogScreenContent(ScreenContent.Confirmation);
          }
        }
        else {
          // Invalid IBAN
          setErrorMessage(ErrorSource.Iban, DisplayState.Show);
        }
      }
      else {
        if (isIbanValid) {
          // Invalid owner name
          setErrorMessage(ErrorSource.Name, DisplayState.Show);
        }
        else {
          // Owner name and IBAN both invalid
          setErrorMessage(ErrorSource.Both, DisplayState.Show);
        }
      }
    }
    else {
      // Duplicate entry confirmation screen being displayed
      String name = getNameInput();
      String iban = getFormattedIbanInput();
      if (mDataManager.replaceAccount(name, iban)) {
        mDataManager.refreshDisplay();
        displaySuccessMessage(SuccessType.Update);
      }
      setNewEditDialogDisplayState(DisplayState.Hide, DialogType.NewAccount);
    }

  }

  public void clearErrorMessage() {
    setErrorMessage(null, DisplayState.Hide);
  }
  private void setErrorMessage(ErrorSource source, DisplayState state) {
    if (state == DisplayState.Show) {
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
  private String getFormattedIbanInput() {
    String iban;
    iban = ibanInputField.getText().toString();
    // Reformat iban and divide into groups, separated by space
    iban = mDataManager.removeSpaces(iban).toUpperCase();
    iban = mDataManager.divideIntoGroups(iban, DataManager.IBAN_GROUPSIZE);
    return iban;
  }
  private String getNameInput() { return nameInputField.getText().toString(); }
  private void displaySuccessMessage(SuccessType type) {
    toastMessage.setText(mActivity.getString(type == SuccessType.New
      ? R.string.newAccount_success
      : R.string.editAccount_success));
    toast.show();
  }

  private void setNewEditDialogScreenContent(ScreenContent content) {
    // Screen content visibilities
    newEditAccountInputScreen.setVisibility(content == ScreenContent.Input
      ? View.VISIBLE
      : View.INVISIBLE);
    newEditAccountConfirmationScreen.setVisibility(content == ScreenContent.Input
      ? View.GONE
      : View.VISIBLE);

    // Button visibilities and texts
    saveButton.setText(content == ScreenContent.Input
      ? R.string.newEditAccount_saveButton
      : R.string.newEditAccount_yesButton);
    cancelButton.setText(content == ScreenContent.Input
      ? R.string.newEditAccount_cancelButton
      : R.string.newEditAccount_noButton);
    deleteButton.setVisibility(View.GONE);
  }

  private void setNewEditDialogDisplayState(DisplayState state, DialogType type) {
    // Show / hide blockers
    for (Button button : guiBlockerButtons) {
      button.setVisibility(state == DisplayState.Show
        ? View.VISIBLE
        : View.INVISIBLE);
    }

    if (state == DisplayState.Show) {
      // Set title
      newEditAccountTitle.setText(
        type == DialogType.NewAccount
          ? R.string.newAccount_title
          : R.string.editAccount_title);

      // Remove error messages
      setErrorMessage(ErrorSource.Both, DisplayState.Hide);

      if (type == DialogType.NewAccount) {
        // Reset input fields and set focus to name field
        nameInputField.setText("");
        nameInputField.requestFocus();
        ibanInputField.setText("");
      }

      // Show / hide delete button
      deleteButton.setVisibility(type == DialogType.NewAccount
        ? View.GONE
        : View.VISIBLE);
    }
    android.util.DisplayMetrics d = new android.util.DisplayMetrics(); mActivity.getWindowManager().getDefaultDisplay().getMetrics(d);
    // Show / hide container
    newEditAccountContainer.setVisibility(
      state == DisplayState.Show ? View.VISIBLE : View.INVISIBLE);
  }
}
