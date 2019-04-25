package com.bitcoin.wallet.mobile.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {AddressBookEntry.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AddressBookDao addressBookDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "address_book")
                            .addMigrations(MIGRATION_1_2).allowMainThreadQueries().build();
                }
            }
        }
        return INSTANCE;
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE address_book_new (address TEXT NOT NULL, label TEXT NULL, PRIMARY KEY(address))");
            database.execSQL(
                    "INSERT OR IGNORE INTO address_book_new (address, label) SELECT address, label FROM address_book");
            database.execSQL("DROP TABLE address_book");
            database.execSQL("ALTER TABLE address_book_new RENAME TO address_book");
        }
    };
}

