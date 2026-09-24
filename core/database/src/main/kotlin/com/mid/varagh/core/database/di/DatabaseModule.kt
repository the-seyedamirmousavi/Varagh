package com.mid.varagh.core.database.di

import android.content.Context
import androidx.room.Room
import com.mid.varagh.core.database.BuildConfig
import com.mid.varagh.core.database.RoomTransactionRunner
import com.mid.varagh.core.database.TransactionRunner
import com.mid.varagh.core.database.VaraghDatabase
import com.mid.varagh.core.database.VaraghMigrations
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VaraghDatabase =
        Room.databaseBuilder(context, VaraghDatabase::class.java, VaraghDatabase.NAME)
            .addMigrations(*VaraghMigrations.ALL)
            .apply {
                // Only debug builds may wipe data, and only when installing an older schema over a
                // newer one. A missing upgrade migration always crashes loudly instead of losing data.
                if (BuildConfig.DEBUG) fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            }
            .build()

    @Provides fun provideBookDao(db: VaraghDatabase) = db.bookDao()
    @Provides fun provideReadingProgressDao(db: VaraghDatabase) = db.readingProgressDao()
    @Provides fun provideReadingSessionDao(db: VaraghDatabase) = db.readingSessionDao()
    @Provides fun provideBookmarkDao(db: VaraghDatabase) = db.bookmarkDao()
    @Provides fun provideUserProfileDao(db: VaraghDatabase) = db.userProfileDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TransactionModule {
    @Binds
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner
}
