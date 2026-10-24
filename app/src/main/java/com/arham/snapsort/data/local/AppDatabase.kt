package com.arham.snapsort.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arham.snapsort.data.local.dao.ScreenshotDao
import com.arham.snapsort.data.local.entity.ScreenshotEntity

@Database(
    entities = [ScreenshotEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun screenshotDao(): ScreenshotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE screenshots ADD COLUMN appConfidence REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE screenshots ADD COLUMN classificationSource TEXT NOT NULL DEFAULT 'auto'")
                db.execSQL("ALTER TABLE screenshots ADD COLUMN category TEXT NOT NULL DEFAULT 'Other'")
                db.execSQL("ALTER TABLE screenshots ADD COLUMN classificationStatus TEXT NOT NULL DEFAULT 'unclassified'")
                db.execSQL("ALTER TABLE screenshots ADD COLUMN matchedKeywords TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_screenshots_category ON screenshots(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_screenshots_chatName ON screenshots(chatName)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ss_organizer.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
