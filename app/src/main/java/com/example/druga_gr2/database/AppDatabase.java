package com.example.druga_gr2.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {OsobaEntity.class, AdresEntity.class}, version = 3, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "BazaOsob2.db";
    private static AppDatabase instance;

    public abstract OsobaDao osobaDao();
    public abstract AdresDao adresDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, DATABASE_NAME)
                    .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3)
                    .build();
        }
        return instance;
    }
}
