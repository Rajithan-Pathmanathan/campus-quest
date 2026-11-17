package com.campusquest

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.campusquest.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration2To3AddsTimerWithoutLosingGames() {
        val name = "migration-2-3"
        helper.createDatabase(name, 2).apply {
            execSQL("INSERT INTO games VALUES ('g1','Quest','Story','u1','Creator','PUBLISHED',3,1,NULL)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(name, 3, true, AppDatabase.MIGRATION_2_3)
        migrated.query("SELECT title, timeLimitMinutes FROM games WHERE id='g1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Quest", cursor.getString(0))
            assertEquals(30, cursor.getInt(1))
        }
        migrated.close()
    }

    @Test
    fun migration2To3AcceptsIntermediateSchemaThatAlreadyHasTimer() {
        val name = "migration-2-3-existing-timer"
        helper.createDatabase(name, 2).apply {
            execSQL("ALTER TABLE games ADD COLUMN timeLimitMinutes INTEGER NOT NULL DEFAULT 30")
            execSQL("INSERT INTO games VALUES ('g2','Quest','Story','u1','Creator','PUBLISHED',3,1,NULL,45)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(name, 3, true, AppDatabase.MIGRATION_2_3)
        migrated.query("SELECT timeLimitMinutes FROM games WHERE id='g2'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(45, cursor.getInt(0))
        }
        migrated.close()
    }
}
