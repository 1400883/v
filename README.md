# IBAN Bookkeeper - bank account number bookkeeping app

### Features
- Supports international IBAN-compliant account numbers
- Stores data in name - account number pairs
- Add / modify / delete entries
- Simple name / IBAN search functionality
- Uses SQLite as permanent data storage
- Looks primitive + virtually no scaling based on display size :(
- To create a few DB entries to start with, uncomment mDataManager.insertFreshDebugData() in AccountListActivity.onCreate().
