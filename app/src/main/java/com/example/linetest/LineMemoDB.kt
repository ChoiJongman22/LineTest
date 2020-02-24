package com.example.linetest

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = arrayOf(LineMemo::class), version = 1)
abstract class LineMemoDB : RoomDatabase() {
    abstract fun lineMemoDao(): LineMemoDao

    companion object {
        private var INSTANCE: LineMemoDB? = null

        fun getInstance(context: Context): LineMemoDB? {
            if (INSTANCE == null) {
                synchronized(LineMemoDB::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        LineMemoDB::class.java, "LineMemo.db"
                    )
                        .allowMainThreadQueries()
                        .build()
                }
            }
            return INSTANCE
        }
    }
}