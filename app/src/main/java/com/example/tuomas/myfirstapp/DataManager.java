package com.example.tuomas.myfirstapp;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class DataManager {
  private final DatabaseAdapter mDatabaseAdapter;
  public final SimpleCursorAdapter mCursorAdapter;

  public DataManager(Activity activity) {

    mCursorAdapter = createCursorAdapter(activity);
    mDatabaseAdapter = new DatabaseAdapter(activity);
  }

  public void openDatabase() {
    mDatabaseAdapter.open();
  }

  public void closeDatabase() {
    mDatabaseAdapter.close();
  }

  private void refreshListview() {
    refreshListview("");
  }

  private Cursor refreshListview(String filter) {
    Cursor cursor = mDatabaseAdapter.fetchAccountsByOwnerOrIban(filter);
    mCursorAdapter.changeCursor(cursor);
    return cursor;
  }

  public void fillListview() {
    // Clean all data
    mDatabaseAdapter.deleteAllAccounts();
    // Add some data
    mDatabaseAdapter.insertSomeAccounts();
    refreshListview();
  }

  public SimpleCursorAdapter getCursorAdapter() {
    return mCursorAdapter;
  }

  public void setListviewDataSource(Activity activity) {
    ((ListView) activity.findViewById(R.id.accountListview))
      .setAdapter(mCursorAdapter);
  }

  public void setListviewFilter() {
    mCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
      public Cursor runQuery(CharSequence constraint) {
        return mDatabaseAdapter.fetchAccountsByOwnerOrIban(constraint.toString());
      }
    });
  }

  private SimpleCursorAdapter createCursorAdapter(Context context) {
    // The desired columns to be bound
    String[] fromColumns = new String[]{
      DatabaseAdapter.COLUMN_OWNER,
      DatabaseAdapter.COLUMN_IBAN,
    };

    // the XML defined views which the data will be bound to
    int[] toViews = new int[]{
      R.id.owner,
      R.id.iban,
    };

    // create the adapter using the cursor pointing to the desired data
    // as well as the layout information
    return new SimpleCursorAdapter(
      context,
      R.layout.account_item,
      null,
      fromColumns,
      toViews,
      0);
  }
}
