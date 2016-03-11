package com.example.tuomas.ibanbookkeeper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseAdapter {

  private DatabaseHelper mDatabaseHelper = null;
  private SQLiteDatabase mDatabase = null;
  private Context mContext;

  //////////////////////////////////////////////////////
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
      COLUMN_OWNER + " TEXT NOT NULL, " +
      COLUMN_IBAN + " TEXT NOT NULL);";

  private static final String DROP_TABLE =
    "DROP TABLE IF EXISTS " + DB_TABLE;
  //////////////////////////////////////////////////////
  public DatabaseAdapter(Context context) { mContext = context; }
  //////////////////////////////////////////////////////
  // Connection management
  //////////////////////////////////////////////////////
  public void open() throws SQLException {
    if (mDatabaseHelper == null) {
      mDatabaseHelper = new DatabaseHelper(mContext);
    }
    mDatabase = mDatabaseHelper.getWritableDatabase();
  }
  public void close() {
    if (mDatabaseHelper != null) {
      mDatabaseHelper.close();
    }
  }
  //////////////////////////////////////////////////////
  // Create, read, update, delete
  //////////////////////////////////////////////////////
  public long createAccount(String owner, String iban) {
    ContentValues values = new ContentValues(2);
    values.put(COLUMN_OWNER, owner);
    values.put(COLUMN_IBAN, iban);
    return mDatabase.insert(DB_TABLE, null, values);
  }
  public Cursor selectAccounts() {
    return selectAccounts("");
  }
  public Cursor selectAccounts(String filter) {
    Cursor cursor = null;
    if (filter == null || filter.length() == 0) {
      // Get all accounts
      cursor = mDatabase.query(DB_TABLE,
        new String[]{COLUMN_ID, COLUMN_OWNER, COLUMN_IBAN},
        null, null, null, null, COLUMN_OWNER + " COLLATE NOCASE");
    } else {
      cursor = mDatabase.query(DB_TABLE,
        new String[]{COLUMN_ID, COLUMN_OWNER, COLUMN_IBAN},
        COLUMN_OWNER + " LIKE ? OR " + COLUMN_IBAN + " LIKE ?",
        new String[]{"%" + filter + "%", "%" + filter + "%"},
        null, null, COLUMN_OWNER + " COLLATE NOCASE");
    }
    if (cursor != null) {
      cursor.moveToFirst();
    }
    return cursor;
  }
  public long replaceAccount(int id, String owner, String iban) {
    ContentValues values = new ContentValues(3);
    values.put(COLUMN_ID, id);
    values.put(COLUMN_OWNER, owner);
    values.put(COLUMN_IBAN, iban);
    return mDatabase.replace(DB_TABLE, null, values);
  }
  public int deleteAccount(int id) {
    return mDatabase.delete(DB_TABLE, "_id == ?",
      new String[] { Integer.toString(id) });
  }
  //////////////////////////////////////////////////////
  // Initial database population for debugging
  //////////////////////////////////////////////////////
  public void insertDebugDataAccounts() {
    createAccount("Pelle Peloton", "GB29 NWBK 6016 1331 9268 19");
    createAccount("Teppo Tulppu", "FI21 1234 5600 0007 85");
    createAccount("Aku Ankka", "TL38 0080 0123 4567 8910 157");
    createAccount("Simo Sisu", "CH93 0076 2011 6238 5295 7");
    createAccount("Musta Pekka", "SI56 2633 0001 2039 086");
    createAccount("Mikki Hiiri", "NO93 8601 1117 947");
  }
  public boolean deleteAllAccounts() { return mDatabase.delete(DB_TABLE, null, null) > 0; }
  //////////////////////////////////////////////////////
  // Database creation helper
  //////////////////////////////////////////////////////
  private class DatabaseHelper extends SQLiteOpenHelper {
    DatabaseHelper(Context context) {
      // Prepare database for creation / upgrade. This will happen on the
      // first call to getWritableDatabase() / getReadableDatabase()
      super(context, DB_NAME, null, DB_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL(CREATE_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL(DROP_TABLE);
      onCreate(db);
    }
  }
}
