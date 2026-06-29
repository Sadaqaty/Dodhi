package com.dodhi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dodhi.data.dao.DodhiDao
import com.dodhi.data.model.Customer
import com.dodhi.data.model.DeliveryRecord
import com.dodhi.data.model.Payment
import com.dodhi.data.model.MilkSource

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Customer::class, DeliveryRecord::class, Payment::class, MilkSource::class], version = 6, exportSchema = false)
abstract class DodhiDatabase : RoomDatabase() {
    abstract fun dodhiDao(): DodhiDao

    fun forceCheckpoint() {
        this.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
    }

    companion object {
        @Volatile
        private var INSTANCE: DodhiDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new 'rate' column with a default of 0.0
                db.execSQL("ALTER TABLE delivery_records ADD COLUMN rate REAL NOT NULL DEFAULT 0.0")
                // Backfill the rate for existing records where quantity > 0
                db.execSQL("UPDATE delivery_records SET rate = amount / quantity WHERE quantity > 0")
            }
        }

        fun getDatabase(context: Context): DodhiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DodhiDatabase::class.java,
                    "dodhi_database"
                )
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
