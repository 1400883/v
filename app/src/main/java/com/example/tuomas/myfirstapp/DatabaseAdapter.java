package com.example.tuomas.myfirstapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.DatabaseUtils;

public class DatabaseAdapter {

  private DatabaseHelper mDatabaseHelper = null;
  private SQLiteDatabase mDatabase = null;
  private final Context mContext;

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
      COLUMN_OWNER + " TEXT NOT NULL, " + COLUMN_IBAN + " TEXT NOT NULL);";

  private static final String DROP_TABLE =
    "DROP TABLE IF EXISTS " + DB_TABLE;
  //////////////////////////////////////////////////////

  public DatabaseAdapter(Context context) { mContext = context; }

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

  public long createAccount(String owner, String iban) {
    ContentValues values = new ContentValues(2);
    values.put(COLUMN_OWNER, owner);
    values.put(COLUMN_IBAN, iban);
    return mDatabase.insert(DB_TABLE, null, values);
  }
  public long replaceAccount(int id, String owner, String iban) {
    ContentValues values = new ContentValues(3);
    values.put(COLUMN_ID, id);
    values.put(COLUMN_OWNER, owner);
    values.put(COLUMN_IBAN, iban);
    return mDatabase.replace(DB_TABLE, null, values);
  }

  public boolean deleteAllAccounts() {
    return mDatabase.delete(DB_TABLE, null, null) > 0;
  }

  public Cursor getAccountsByOwnerOrIban(String filter) throws SQLException {
    Cursor mCursor = null;
    if (filter == null || filter.length() == 0) {
      // Get all accounts
      mCursor = mDatabase.query(DB_TABLE,
        new String[]{COLUMN_ID, COLUMN_OWNER, COLUMN_IBAN},
        null, null, null, null, COLUMN_OWNER + " COLLATE NOCASE");
    } else {
      mCursor = mDatabase.query(DB_TABLE,
        new String[]{COLUMN_ID, COLUMN_OWNER, COLUMN_IBAN},
        COLUMN_OWNER + " LIKE ? OR " + COLUMN_IBAN + " LIKE ?",
        new String[]{"%" + filter + "%", "%" + filter + "%"},
        null, null, COLUMN_OWNER + " COLLATE NOCASE");
    }
    if (mCursor != null) {
      mCursor.moveToFirst();
    }
    return mCursor;
  }

  public void insertSomeAccounts() {
    createAccount("Pelle Peloton", "FI45 5390 0050 0045 03");
    createAccount("Teppo Tulppu", "FI45 5390 0050 0045 04");
    createAccount("Kalle Korkki", "FI45 5390 0050 0045 05");
    createAccount("Aku Ankka", "FI45 5390 0050 0045 06");
    createAccount("Simo Sisu", "FI45 5390 0050 0045 07");
    createAccount("Musta Pekka", "FI45 5390 0050 0045 08");
    createAccount("Mikki Hiiri", "FI45 5390 0050 0045 09");
  }

  private class DatabaseHelper extends SQLiteOpenHelper {
    DatabaseHelper(Context context) {
      // Prepare database for creation. This will happen on the
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
