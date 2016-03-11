package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import java.util.Arrays;

public class DataManager {
  private DatabaseAdapter mDatabaseAdapter;
  private SimpleCursorAdapter mCursorAdapter;

  private String TAG = "DataManager";
  private Activity mActivity;
  private GuiManager mGuiManager;
  private EventManager mEventManager;
  private SessionStorage mSessionStorage;
  private static DataManager instance;

  private static String[] fromColumns;
  private static int[] toViews;
  public static final int MAX_OWNER_LENGTH = 30;

  private int IBAN_GROUPSIZE = 4;

  static {
    // SimpleCursorAdapter source columns
    fromColumns = new String[]{
      DatabaseAdapter.COLUMN_OWNER,
      DatabaseAdapter.COLUMN_IBAN,
    };
    // SimpleCursorAdapter target views
    toViews = new int[]{
      R.id.owner,
      R.id.iban
    };
  }

  public SessionStorage saveSession() {
    EditText nameInput = (EditText)mActivity.findViewById(R.id.nameInputField);
    EditText ibanInput = (EditText)mActivity.findViewById(R.id.ibanInputField);

    mSessionStorage.setCurrentName(nameInput.getText().toString());
    mSessionStorage.setCurrentIban(ibanInput.getText().toString());
    // Original name that the user may have replaced in Edit account screen will
    // be (and by this point in execution, has been) stored by GuiManager as needed
    return mSessionStorage;
  }
  public void restoreSession(Bundle bundle) {
    if (bundle != null) {
      mSessionStorage = (SessionStorage)bundle.getSerializable("session");
      switch (mGuiManager.getGuiState()) {
        case New:
          mGuiManager.updateScreen(
            GuiManager.NewDisplayState.Show, GuiManager.RequestedScreen.New);
        case Edit:
          mGuiManager.updateScreen(
            GuiManager.NewDisplayState.Show, GuiManager.RequestedScreen.Edit);
          break;
        case Confirm:
          boolean wasNewState =
            mGuiManager.getPreviousGuiState() == GuiManager.GuiState.New;
          String currentName = mSessionStorage.getCurrentName();
          mGuiManager.updateScreen(
            GuiManager.NewDisplayState.Show,
            GuiManager.RequestedScreen.Confirm,
            currentName,
            wasNewState
              ? mActivity.getString(R.string.newAccount_duplicateTitle)
              : mActivity.getString(R.string.editAccount_duplicateTitle),
            getIbanFromName(currentName),
            formatIban(mSessionStorage.getCurrentIban()));
          break;
        case Delete:
          String originalName = mSessionStorage.getOriginalName();
          mGuiManager.updateScreen(
            GuiManager.NewDisplayState.Show,
            GuiManager.RequestedScreen.Delete,
            originalName,
            mActivity.getString(R.string.deleteAccount_confirmationTitle),
            getIbanFromName(originalName));
          break;
      }
    }
  }
  ///////////////////////////////////////////////
  // Singleton setup
  ///////////////////////////////////////////////
  public static DataManager get() {
    if (instance == null) {
      instance = new DataManager();
    }
    return instance;
  }
  private DataManager() {}
  public void updateReferences(Activity activity) {
    if (mActivity != activity) {
      mActivity = activity;
      if (mGuiManager == null || mEventManager == null) {
        mGuiManager = GuiManager.get();
        mEventManager = EventManager.get();
      }
      if (mSessionStorage == null) {
        mSessionStorage = new SessionStorage();
      }
      updateAdapters();
    }
  }
  private void updateAdapters() {
    // Needs to be always recreated on activity change
    mDatabaseAdapter = new DatabaseAdapter(mActivity);

    // Create cursor adapter initially without a cursor.
    // Bind source columns & target layout and views.
    mCursorAdapter = new SimpleCursorAdapter(
      mActivity,
      R.layout.listview_item,
      null,
      fromColumns,
      toViews,
      0);
    setCursorAdapterDataSource();
    setCursorAdapterResultFilter();
  }
  private void setCursorAdapterDataSource() {
    ((ListView)mActivity.findViewById(R.id.listview))
      .setAdapter(mCursorAdapter);
  }
  private void setCursorAdapterResultFilter() {
    mCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
      public Cursor runQuery(CharSequence constraint) {
        return mDatabaseAdapter.getAccountsByOwnerOrIban(constraint.toString());
      }
    });
  }
  ///////////////////////////////////////////////
  public void openDatabase() { mDatabaseAdapter.open(); }
  public void closeDatabase() { mDatabaseAdapter.close(); }
  ///////////////////////////////////////////////
  public void insertFreshDebugData() {
    // Clean all data from database
    mDatabaseAdapter.deleteAllAccounts();
    // Add some data into database
    mDatabaseAdapter.insertDebugDataAccounts();
    // Update data display
    mCursorAdapter.changeCursor(mDatabaseAdapter.getAccountsByOwnerOrIban());
  }
  ///////////////////////////////////////////////
  public void refreshAccountScreenView() {
    // Invoke search bar callback manually with current
    // filter text to force listview display update.
    CharSequence text = ((EditText)mActivity.findViewById(R.id.searchField)).getText();
    mEventManager.onTextChanged(text, 0, 0, 0);
  }
  ///////////////////////////////////////////////
  public boolean createAccount(String name, String iban) {
    return mDatabaseAdapter.createAccount(name, iban) > -1;
  }
  public boolean replaceAccount(int id, String name, String iban) {
    return mDatabaseAdapter.replaceAccount(id, name, iban) > -1;
  }
  public boolean deleteAccount(String name) {
    int id = getIdFromName(name);
    return mDatabaseAdapter.deleteAccount(id) == 1;
  }
  ///////////////////////////////////////////////
  public SimpleCursorAdapter getCursorAdapter() { return mCursorAdapter; }
  ///////////////////////////////////////////////
  public void setOriginalName(String name) { mSessionStorage.setOriginalName(name); }
  public String getOriginalName() { return mSessionStorage.getOriginalName(); }
  public boolean isSameAsOriginalName(String name) {
    return mSessionStorage.getOriginalName().equals(name);
  }
  public boolean isUniqueName(String name) {
    Cursor cursor = mDatabaseAdapter.getAccountsByOwnerOrIban();
    cursor.moveToFirst();
    for (int i = 0; i < cursor.getCount(); ++i) {
      String owner = cursor.getString(cursor.getColumnIndex(DatabaseAdapter.COLUMN_OWNER));
      // Let case-sensitive comparison be fine.
      if (owner.equals(name))
        return false;
      cursor.moveToNext();
    }
    return true;
  }
  public int getIdFromName(String name) {
    Cursor cursor = mDatabaseAdapter.getAccountsByOwnerOrIban();
    cursor.moveToFirst();
    for (int i = 0; i < cursor.getCount(); ++i) {
      String owner = cursor.getString(cursor.getColumnIndex(DatabaseAdapter.COLUMN_OWNER));
      if (owner.equals(name)) {
        return cursor.getInt(cursor.getColumnIndex(DatabaseAdapter.COLUMN_ID));
      }
      cursor.moveToNext();
    }
    return -1;
  }
  public String getIbanFromName(String name) /*throws Exception */ {
    Cursor cursor =  mDatabaseAdapter.getAccountsByOwnerOrIban();
    cursor.moveToFirst();
    for (int i = 0; i < cursor.getCount(); ++i) {
      String owner = cursor.getString(cursor.getColumnIndex(DatabaseAdapter.COLUMN_OWNER));
      if (owner.equals(name)) {
        return cursor.getString(cursor.getColumnIndex(DatabaseAdapter.COLUMN_IBAN));
      }
      cursor.moveToNext();
    }
    Log.d(TAG, "Owner '" + name + "' did not match any account.");
    // throw new Exception("Owner '" + name + "' did not match any account.");
    return "";
  }
  ///////////////////////////////////////////////
  public String formatIban(String iban) {
    // Reformat iban:
    // - remove spaces
    // - convert to uppercase
    // - divide into groups, each separated by space
    return
      removeWhitespaces(iban)
      .toUpperCase()
      .replaceAll("(.{0," + IBAN_GROUPSIZE + "})", "$1 ").trim();
  }
  public String removeWhitespaces(String input) { return input.replaceAll("\\s", ""); }
  ///////////////////////////////////////////////

  ///////////////////////////////////////////////
  // Input validator inner class
  ///////////////////////////////////////////////
  public static class InputValidator {
    private static final String countryCodes[];
    private static final int lengths[];
    private static final String countryCodeRegex;
    private static final String chars;
    static {
      countryCodeRegex =
        "AL|AD|AT|AZ|BH|BE|BA|BR|BG|CR|HR|CY|CZ|DK|DO|EE|FI|FR|GE|DE|GI|GR|" +
        "GT|HU|IS|IE|IL|IT|JO|KZ|XK|KW|LV|LB|LI|LT|LU|MK|MT|MR|MU|MD|MC|ME|" +
        "NL|NO|PK|PS|PL|PT|QA|RO|LC|SM|ST|SA|RS|SC|SK|SI|ES|SE|CH|TL|TN|TR|" +
        "UA|AE|GB|VG";
      countryCodes = countryCodeRegex.split("\\|");
      lengths = new int[] {
        28,24,20,28,22,16,20,29,22,21,21,28,24,18,28,20,18,27,22,22,23,27,
        28,28,26,22,23,27,30,20,20,30,21,28,21,20,20,19,31,27,30,24,27,22,
        18,15,24,29,28,25,29,24,32,27,25,24,22,31,24,19,24,24,21,23,24,26,
        29,23,22,24
      };

      chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    }
    //////////////////////////////////////////////////////
    public static boolean isValidName(String name) {
      // Non null and else than empty / only whitespaces
      return name != null && removeWhitespaces(name).length() > 0;
    }
    public static boolean isValidIban(String iban) {
      if (iban != null) {
        // Remove whitespaces and convert to uppercase first
        iban = removeWhitespaces(iban).toUpperCase();
        return
          isValidCountryCode(iban) &&
          isValidLength(iban) &&
          isValidCharacterSet(iban) &&
          isValidCheckDigitSet(iban);
      }
      return false;
    }
    //////////////////////////////////////////////////////
    private static boolean isValidCountryCode(String iban) {
      String cc = getCountryCode(iban);
      Log.d("isValidCountryCode", Integer.toString(getCountryCodeIndex(cc)));
      return getCountryCodeIndex(cc) > -1;
    }
    private static int getCountryCodeIndex(String cc) {
      return Arrays.asList(countryCodes).indexOf(cc);
    }
    private static boolean isValidLength(String iban) {
      Log.d("isValidLength", iban.length() + " " + getCorrectLength(iban));
      return iban.length() == getCorrectLength(iban);
    }
    private static boolean isValidCharacterSet(String iban) {
      Log.d("isValidCharacterSet", Boolean.toString(iban.matches("^[0-9A-Z]+$")));
      return iban.matches("^[0-9A-Z]+$");
    }
    private static boolean isValidCheckDigitSet(String iban) {
    /*
      http://www.cnb.cz/cs/platebni_styk/iban/download/EBS204.pdf, chapter 6.1
    */
      iban = moveFirstFourCharsToEnd(iban);
      iban = convertAlphabetToNumbers(iban);
      int modulo = applyMod97(iban);
      Log.d("isValidCheckDigitSet", Integer.toString(modulo));
      return modulo == 1;
    }

    private static int applyMod97(String iban) {
    /*
      http://www.cnb.cz/cs/platebni_styk/iban/download/EBS204.pdf, chapter 6.3
    */
      int modulo = 0;
      // Process in 9 digit chunks, compatible with 32-bit signed int precision
      int numDigitsPerChunk = 9;
      while (iban.length() > 0) {
        // Extract another chunk
        int chunkLength = Math.min(numDigitsPerChunk, iban.length());
        int currentChunk = Integer.parseInt(iban.substring(0, chunkLength));
        // Trim chunk length worth of digits from left or all if shorter
        iban = iban.replaceAll("^.{" + chunkLength + "}(.*)$", "$1");
        modulo = currentChunk % 97;
        // Add modulo before the remainder
        if (modulo > 0 && iban.length() > 0)
          iban = Integer.toString(modulo) + iban;
      }
      return modulo;
    }

    private static String moveFirstFourCharsToEnd(String iban) {
      return iban.replaceAll("^(.{4})(.*)$", "$2$1");
    }

    private static String convertAlphabetToNumbers(String iban) {
      for (int i = 0; i < iban.length(); ++i) {
        char currentChar = iban.charAt(i);
        int index = chars.indexOf(currentChar);
        if (index > -1) {
          int replacementValue = (index + 10);
          iban = iban.replaceAll(
            Character.toString(currentChar),
            Integer.toString(replacementValue));
        }
      }
      return iban;
    }

    private static String getCountryCode(String iban) {
      return iban.substring(0, Math.min(2, iban.length()));
    }

    private static int getCorrectLength(String iban) {
      String cc = getCountryCode(iban);
      int ccIndex = getCountryCodeIndex(cc);
      return ccIndex > -1 ? lengths[ccIndex] : -1;
    }

    private static String removeWhitespaces(String iban) {
      return iban.replaceAll("\\s", "");
    }
  }
}
