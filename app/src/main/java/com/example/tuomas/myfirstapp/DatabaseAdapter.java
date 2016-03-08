package com.example.tuomas.myfirstapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseAdapter {

  private DatabaseHelper mDbHelper = null;
  private SQLiteDatabase mDb = null;
  private final Context mContext;

  private static final String TAG = "DatabaseAdapter";

  // Database specs
  //////////////////////////////////////////////////////
  private static final String DB_NAME = "account.db";
  private static final String DB_TABLE = "account";
  private static final int DB_VERSION = 1;

  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_OWNER = "owner";
  public static final String COLUMN_IBAN = "iban";

  private static final String CREATE_TABLE =
    "CREATE TABLE IF NOT EXISTS " + DB_TABLE + " (" +
      COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
      COLUMN_OWNER + " TEXT NOT NULL, " + COLUMN_IBAN + " TEXT NOT NULL);";

  private static final String DROP_TABLE =
    "DROP TABLE IF EXISTS " + DB_TABLE;
  //////////////////////////////////////////////////////

  private static class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context) {
      // Prepare database for creation. This will happen on the
      // first call to getWritableDatabase() / getReadableDatabase()
      super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      Log.w(TAG, CREATE_TABLE);
      db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
        + newVersion + ", which will destroy all old data");
      db.execSQL(DROP_TABLE);
      onCreate(db);
    }
  }

  public DatabaseAdapter(Context ctx) {
    this.mContext = ctx;
  }

  public void open() throws SQLException {
    if (mDbHelper == null) {
      mDbHelper = new DatabaseHelper(mContext);
    }
    mDb = mDbHelper.getWritableDatabase();
  }

  public void close() {
    if (mDbHelper != null) {
      mDbHelper.close();
    }
  }

  public long createAccount(String owner, String iban) {
    ContentValues initialValues = new ContentValues();
    initialValues.put(COLUMN_OWNER, owner);
    initialValues.put(COLUMN_IBAN, iban);

    return mDb.insert(DB_TABLE, null, initialValues);
  }

  public boolean deleteAllAccounts() {
    return mDb.delete(DB_TABLE, null, null) > 0;
  }

  public Cursor fetchAccountsByOwnerOrIban(String inputText) throws SQLException {
    Log.w(TAG, inputText);
    Cursor mCursor = null;
    if (inputText == null || inputText.length() == 0) {
      // Fetch all accounts
      mCursor = mDb.query(DB_TABLE,
        new String[]{COLUMN_ID, COLUMN_OWNER, COLUMN_IBAN},
        null, null, null, null, COLUMN_OWNER);
    } else {
      mCursor = mDb.query(DB_TABLE,
        new String[]{COLUMN_ID, COLUMN_OWNER, COLUMN_IBAN},
        COLUMN_OWNER + " LIKE ? OR " + COLUMN_IBAN + " LIKE ?",
        new String[]{"%" + inputText + "%", "%" + inputText + "%"},
        null, null, COLUMN_OWNER);
    }
    if (mCursor != null) {
      mCursor.moveToFirst();
    }
    return mCursor;
  }

  public void insertSomeAccounts() {
    createAccount("Tuomas Keinänen", "FI45 5390 0050 0045 03");
    createAccount("Tuomas Keinänen", "FI45 5390 0050 0045 04");
    createAccount("Tuomas Keinänen", "FI45 5390 0050 0045 05");
    createAccount("Tuomas Keinänen", "FI45 5390 0050 0045 06");
    createAccount("Tuomas Keinänen", "FI45 5390 0050 0045 07");
    createAccount("Tuomas Keinänen", "FI45 5390 0050 0045 08");
    createAccount("Tuomas Keinänen", "FI45 5390 0050 0045 09");
  }
}