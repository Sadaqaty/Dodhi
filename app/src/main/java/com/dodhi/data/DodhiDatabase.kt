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

@Database(entities = [Customer::class, DeliveryRecord::class, Payment::class, MilkSource::class], version = 3, exportSchema = false)
abstract class DodhiDatabase : RoomDatabase() {
    abstract fun dodhiDao(): DodhiDao

    companion object {
        @Volatile
        private var INSTANCE: DodhiDatabase? = null

        fun getDatabase(context: Context): DodhiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DodhiDatabase::class.java,
                    "dodhi_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
