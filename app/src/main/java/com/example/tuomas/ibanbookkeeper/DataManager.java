package com.example.tuomas.ibanbookkeeper;

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
  private String TAG = "DataManager";

  // Database access and data binding to listview
  private DatabaseAdapter mDatabaseAdapter;
  private SimpleCursorAdapter mCursorAdapter;
  private static String[] fromColumns;
  private static int[] toViews;

  // Additional data storage for state
  // recovery across activity restarts
  private SessionStorage mSessionStorage;

  private Activity mActivity;
  private GuiManager mGuiManager;
  private EventManager mEventManager;
  private static DataManager instance;

  // Account owner input field max chars
  public static final int MAX_OWNER_LENGTH = 30;
  // IBAN grouping size for easier visualization, space divided
  private final int IBAN_GROUPSIZE = 4;

  static {
    // SimpleCursorAdapter source columns
    fromColumns = new String[] {
      DatabaseAdapter.COLUMN_OWNER,
      DatabaseAdapter.COLUMN_IBAN,
    };
    // SimpleCursorAdapter target views
    toViews = new int[] {
      R.id.owner,
      R.id.iban
    };
  }
  ///////////////////////////////////////////////
  // Singleton
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
      // Update activity references on activity restart
      updateAdapters();
    }
  }
  private void updateAdapters() {
    // Needs to be always recreated on activity change
    // (SQLiteOpenHelper requires context)
    mDatabaseAdapter = new DatabaseAdapter(mActivity);

    // Create cursor adapter initially without a cursor,
    // which will be set later.
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
  ///////////////////////////////////////////////
  // SimpleCursorAdapter setup
  ///////////////////////////////////////////////
  private void setCursorAdapterDataSource() {
    // Bind adapter to listview
    ((ListView)mActivity.findViewById(R.id.listview))
      .setAdapter(mCursorAdapter);
  }
  private void setCursorAdapterResultFilter() {
    // Setup filtering callback, which will
    // return filterer data set from the database
    mCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
      public Cursor runQuery(CharSequence constraint) {
        return mDatabaseAdapter.selectAccounts(constraint.toString());
      }
    });
  }
  ///////////////////////////////////////////////
  // Session storage
  ///////////////////////////////////////////////
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
      if (mSessionStorage != null) {
        switch (mGuiManager.getGuiState()) {
          case New:
            mGuiManager.updateScreen(
              GuiManager.NewDisplayState.Show, GuiManager.RequestedScreen.New);
            break;
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
      else {
        Log.d("TAG", "null session storage");
      }
    }
  }
  ///////////////////////////////////////////////
  // Database connection redirection
  ///////////////////////////////////////////////
  public void openDatabase() { mDatabaseAdapter.open(); }
  public void closeDatabase() { mDatabaseAdapter.close(); }
  ///////////////////////////////////////////////
  // Database command redirection
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
  // Initial database data creation for testing / debugging purposes
  ///////////////////////////////////////////////
  public void insertFreshDebugData() {
    // Clean all data from database
    mDatabaseAdapter.deleteAllAccounts();
    // Add some data into database
    mDatabaseAdapter.insertDebugDataAccounts();
    // Update data display
    mCursorAdapter.changeCursor(mDatabaseAdapter.selectAccounts());
  }
  ///////////////////////////////////////////////
  // Listview display update
  ///////////////////////////////////////////////
  public void updateResultsToScreen() {
    // Invoke search bar callback manually with current
    // filter text to force listview display update.
    CharSequence text = ((EditText)mActivity.findViewById(R.id.searchField)).getText();
    mEventManager.onTextChanged(text, 0, 0, 0);
  }
  ///////////////////////////////////////////////
  // Public getter for cursor adapter
  ///////////////////////////////////////////////
  public SimpleCursorAdapter getCursorAdapter() { return mCursorAdapter; }
  ///////////////////////////////////////////////
  // Name & IBAN storage / treatment / validation
  ///////////////////////////////////////////////
  public void setOriginalName(String name) { mSessionStorage.setOriginalName(name); }
  public String getOriginalName() { return mSessionStorage.getOriginalName(); }
  ///////////////////////////////////////////////
  public boolean isSameAsOriginalName(String name) {
    return mSessionStorage.getOriginalName().equals(name);
  }
  public boolean isUniqueName(String name) {
    Cursor cursor = mDatabaseAdapter.selectAccounts();
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
    Cursor cursor = mDatabaseAdapter.selectAccounts();
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
  public String getIbanFromName(String name) {
    Cursor cursor =  mDatabaseAdapter.selectAccounts();
    cursor.moveToFirst();
    for (int i = 0; i < cursor.getCount(); ++i) {
      String owner = cursor.getString(cursor.getColumnIndex(DatabaseAdapter.COLUMN_OWNER));
      if (owner.equals(name)) {
        return cursor.getString(cursor.getColumnIndex(DatabaseAdapter.COLUMN_IBAN));
      }
      cursor.moveToNext();
    }
    Log.d(TAG, "Owner '" + name + "' did not match any account.");
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
    private static final String ibanCountryCodes[];
    private static final int ibanLengths[];
    private static final String countryCodeRegex;
    private static final String chars;
    static {
      // As listed @ https://www.swift.com/sites/default/files/resources/swift_standards_ibanregistry.pdf
      countryCodeRegex =
        "AD|AE|AL|AT|AX|AZ|BA|BE|BG|BH|BR|CH|CR|CY|CZ|DE|DK|DO|EE|ES|FI|FO|" +
        "FR|GB|GE|GF|GI|GL|GP|GR|GT|HR|HU|IE|IL|IS|IT|JO|KW|KZ|LB|LC|LI|LT|" +
        "LU|LV|MC|MD|ME|MK|MQ|MR|MT|MU|NL|NO|PK|PL|PM|PS|PT|QA|RE|RO|RS|SA|" +
        "SC|SE|SI|SK|SM|ST|TL|TN|TR|UA|VG|XK|YT";
      ibanCountryCodes = countryCodeRegex.split("\\|");
      ibanLengths = new int[] {
         24,23,28,20,18,28,20,16,22,22,29,21,21,28,24,22,18,28,20,24,18,18,
         27,22,22,27,23,18,27,27,28,21,28,22,23,26,27,30,30,20,28,32,21,20,
         20,21,27,24,22,19,27,27,31,30,18,15,24,28,27,29,25,29,27,24,22,24,
         31,24,19,24,27,25,23,24,26,29,24,20,27
      };

      chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    }
    ///////////////////////////////////////////////
    // Public validation methods
    ///////////////////////////////////////////////
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
    ///////////////////////////////////////////////
    // Validation implementation
    ///////////////////////////////////////////////
    private static boolean isValidCountryCode(String iban) {
      String cc = getCountryCode(iban);
      Log.d("isValidCountryCode", Integer.toString(getCountryCodeIndex(cc)));
      return getCountryCodeIndex(cc) > -1;
    }
    private static int getCountryCodeIndex(String cc) {
      return Arrays.asList(ibanCountryCodes).indexOf(cc);
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
    /*
    http://www.cnb.cz/cs/platebni_styk/iban/download/EBS204.pdf, chapter 6.3
    */
      // Usually A-Z have consecutive ascii positions, but by using own
      // char array as a base this makes a 100% compatible implementation
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
      return ccIndex > -1 ? ibanLengths[ccIndex] : -1;
    }

    private static String removeWhitespaces(String iban) {
      return iban.replaceAll("\\s", "");
    }
  }
}
