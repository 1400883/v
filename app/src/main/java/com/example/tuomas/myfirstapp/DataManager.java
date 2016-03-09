package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import java.util.Arrays;

public class DataManager {
  private DatabaseAdapter mDatabaseAdapter;
  private SimpleCursorAdapter mCursorAdapter;
  // private InputValidator mInputValidator;

  private static Activity mActivity;
  private GuiManager mGuiManager;
  private EventManager mEventManager;
  private static DataManager instance = null;
  public final static int IBAN_GROUPSIZE = 4;
  ///////////////////////////////////////////////
  // Singleton setup. Reference to activity and other managers
  // must be set before requesting instance via singleton
  ///////////////////////////////////////////////

  public static void setActivity(Activity activity) {
    mActivity = activity;
  }
  public void setManagers(EventManager eventManager, GuiManager guiManager) {
    mGuiManager = guiManager;
    mEventManager = eventManager;
  }

  public static DataManager get() {
    if (instance == null) {
      instance = new DataManager();
    }
    return instance;
  }

  ///////////////////////////////////////////////
  // Constructor
  ///////////////////////////////////////////////
  private DataManager() {
    mCursorAdapter = createCursorAdapter(mActivity);
    mDatabaseAdapter = new DatabaseAdapter(mActivity);
    // mInputValidator = new InputValidator();
  }

  ///////////////////////////////////////////////

  public void openDatabase() {
    mDatabaseAdapter.open();
  }
  public void closeDatabase() {
    mDatabaseAdapter.close();
  }

  private void refreshDataDisplay() { refreshDataDisplay(""); }

  private Cursor refreshDataDisplay(String filter) {
    Cursor cursor = mDatabaseAdapter.getAccountsByOwnerOrIban(filter);
    mCursorAdapter.changeCursor(cursor);
    return cursor;
  }

  public void populateData() {
    // Clean all data from database
    mDatabaseAdapter.deleteAllAccounts();
    // Add some data into database
    mDatabaseAdapter.insertSomeAccounts();
    // Update data display
    refreshDataDisplay();
  }
  public void refreshDisplay() {
    // Invoke search bar callback manually with current
    // filter text to force listview display update.
    CharSequence text = ((EditText)mActivity.findViewById(R.id.searchField)).getText();
    mEventManager.onTextChanged(text, 0, 0, 0);
  }
  public boolean createAccount(String name, String iban) {
    return mDatabaseAdapter.createAccount(name, iban) > -1;
  }
  public boolean replaceAccount(String name, String iban) {
    int id = getIdFromName(name);
    return mDatabaseAdapter.replaceAccount(id, name, iban) > -1;
  }
  public SimpleCursorAdapter getCursorAdapter() { return mCursorAdapter; }
  public void setDataSource() {
    ((ListView)mActivity.findViewById(R.id.listview))
      .setAdapter(mCursorAdapter);
  }
  public void setResultFilter() {
    mCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
      public Cursor runQuery(CharSequence constraint) {
        return mDatabaseAdapter.getAccountsByOwnerOrIban(constraint.toString());
      }
    });
  }

  private SimpleCursorAdapter createCursorAdapter(Context context) {
    // Source columns
    String[] fromColumns = new String[]{
      DatabaseAdapter.COLUMN_OWNER,
      DatabaseAdapter.COLUMN_IBAN,
    };

    // Target views
    int[] toViews = new int[]{
      R.id.owner,
      R.id.iban,
    };

    // Create the adapter initially without a cursor.
    // Bind source columns & target layout and views.
    return new SimpleCursorAdapter(
      context,
      R.layout.account_item,
      null,
      fromColumns,
      toViews,
      0);
  }

  public boolean isDuplicateName(String name) {
    Cursor cursor = mCursorAdapter.getCursor();
    cursor.moveToFirst();
    for (int i = 0; i < cursor.getCount(); ++i) {
      String owner = cursor.getString(cursor.getColumnIndex(DatabaseAdapter.COLUMN_OWNER));
      // Let case-sensitive comparison be fine.
      if (owner.equals(name))
        return true;
      cursor.moveToNext();
    }
    return false;
  }

  private int getIdFromName(String name) {
    Cursor cursor = mCursorAdapter.getCursor();
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

  public String getIbanFromName(String name) throws Exception {
    Cursor cursor = mCursorAdapter.getCursor();
    cursor.moveToFirst();
    for (int i = 0; i < cursor.getCount(); ++i) {
      String owner = cursor.getString(cursor.getColumnIndex(DatabaseAdapter.COLUMN_OWNER));
      if (owner.equals(name)) {
        return cursor.getString(cursor.getColumnIndex(DatabaseAdapter.COLUMN_IBAN));
      }
      cursor.moveToNext();
    }
    throw new Exception("Owner '" + name + "' did not match any account.");
  }

  public String removeSpaces(String iban) {
    return iban.replaceAll("\\s", "");
  }
  public String divideIntoGroups(String iban, int groupSize) {
    return iban.replaceAll("(.{0," + groupSize + "})", "$1 ").trim();
  }

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
    // Public methods
    //////////////////////////////////////////////////////
    public static boolean isValidName(String name) {
      // Non null and else than empty / only whitespaces
      return name != null && name.replaceAll("\\s", "").length() > 0;
    }

    public static boolean isValidIban(String iban) {
      if (iban != null) {
        iban = DataManager.get().removeSpaces(iban).toUpperCase();
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
      Log.w("isValidCountryCode", Integer.toString(getCountryCodeIndex(cc)));
      return getCountryCodeIndex(cc) > -1;
    }
    private static int getCountryCodeIndex(String cc) {
      return Arrays.asList(countryCodes).indexOf(cc);
    }
    private static boolean isValidLength(String iban) {
      Log.w("isValidLength", iban.length() + " " + getCorrectLength(iban));
      return iban.length() == getCorrectLength(iban);
    }
    private static boolean isValidCharacterSet(String iban) {
      Log.w("isValidCharacterSet", Boolean.toString(iban.matches("^[0-9A-Z]+$")));
      return iban.matches("^[0-9A-Z]+$");
    }
    private static boolean isValidCheckDigitSet(String iban) {
    /*
      http://www.cnb.cz/cs/platebni_styk/iban/download/EBS204.pdf, chapter 6.1
    */
      iban = moveFirstFourCharsToEnd(iban);
      iban = convertAlphabetToNumbers(iban);
      int modulo = applyMod97(iban);
      Log.w("isValidCheckDigitSet", Integer.toString(modulo));
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
  }
}
