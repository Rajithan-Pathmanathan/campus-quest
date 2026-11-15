package com.campusquest

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.campusquest.data.local.AppDatabase
import com.campusquest.data.local.entity.*
import com.campusquest.data.repository.GameRepositoryImpl
import com.campusquest.domain.model.*
import com.campusquest.domain.repository.AuthRepository
import com.campusquest.story.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoryDatabaseTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val auth=object:AuthRepository {
        override val currentUserState=MutableStateFlow<AuthUser?>(AuthUser("player","Player",null))
        override fun getCurrentUserId()="player"
        override suspend fun signOut(){}
        override suspend fun deleteAccount()=Result.success(Unit)
        override suspend fun signInWithEmail(email:String,password:String)=Result.failure<AuthUser>(UnsupportedOperationException())
        override suspend fun signUpWithEmail(email:String,password:String,displayName:String)=Result.failure<AuthUser>(UnsupportedOperationException())
        override suspend fun signInAnonymously()=Result.failure<AuthUser>(UnsupportedOperationException())
    }
    private suspend fun seed(db:AppDatabase):String=LightTheWay.create(db,"player","Player",listOf(7.0 to 81.0,7.001 to 81.0,7.002 to 81.0),0f)
    @Test fun discoveryAndOutboxRollbackTogetherAndConcurrentClaimsAwardOnce()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            val id=seed(db)
            val repo=GameRepositoryImpl(db.gameDao(),db.checkpointDao(),db.playerProgressDao(),db.syncQueueDao(),auth,
                inTransaction={block ->db.withTransaction {block()}})
            val cp=db.checkpointDao().getCheckpointsForGame(id).first()
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_queue BEFORE INSERT ON sync_queue BEGIN SELECT RAISE(ABORT, 'test queue failure'); END")
            assertTrue(repo.recordDiscovery(id,cp.id,1000).isFailure)
            assertNull(db.playerProgressDao().getProgress(id,"player"))
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_queue")
            assertTrue(repo.recordDiscovery(id,cp.id,1000).isSuccess)
            assertTrue(repo.recordDiscovery(id,cp.id,1000).isSuccess)
            assertEquals(100,db.playerProgressDao().getProgress(id,"player")!!.score)
            assertEquals(1,db.syncQueueDao().getPendingSyncItems().size)
            val old=db.gameDao().getGameById(id)!!
            db.gameDao().insertGame(old.copy(title="Edited title"))
            assertEquals(3,db.checkpointDao().getCheckpointsForGame(id).size)
            db.checkpointDao().deleteCheckpoint(cp.id)
            val second=db.checkpointDao().getCheckpointsForGame(id).first()
            coroutineScope {
                listOf(async { repo.recordDiscovery(id,second.id,2000) },async { repo.recordDiscovery(id,second.id,2000) }).awaitAll().forEach { assertTrue(it.isSuccess) }
            }
            assertEquals(200,db.playerProgressDao().getProgress(id,"player")!!.score)
            assertFalse(db.playerProgressDao().getProgress(id,"player")!!.isCompleted)
        } finally {db.close()}
    }
    @Test fun migrationPreservesExistingGameAndProgress()=runBlocking {
        val name="story-migration-${System.nanoTime()}.db"
        var db=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
        val id=seed(db)
        db.playerProgressDao().insertOrUpdateProgress(PlayerProgressEntity(id,"player",emptyList(),1,0,1000,null,false))
        db.close()
        val raw=android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,0)
        raw.execSQL("DROP TABLE story_config");raw.execSQL("DROP TABLE story_session");raw.version=1;raw.close()
        db=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(AppDatabase.STORY_MIGRATION).build()
        try {
            assertNotNull(db.gameDao().getGameById(id))
            assertEquals(3,db.checkpointDao().getCheckpointsForGame(id).size)
            assertNotNull(db.playerProgressDao().getProgress(id,"player"))
            assertNull(db.storyDao().config(id))
        } finally {db.close();context.deleteDatabase(name)}
    }
}

