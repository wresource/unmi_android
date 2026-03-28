package io.unmi.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.unmi.app.data.local.datastore.dataStore
import io.unmi.app.data.local.db.AppDatabase
import io.unmi.app.data.local.db.dao.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "unmi_database"
        )
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideDomainDao(db: AppDatabase): DomainDao = db.domainDao()

    @Provides
    fun provideRenewalRecordDao(db: AppDatabase): RenewalRecordDao = db.renewalRecordDao()

    @Provides
    fun provideSyncBackupLogDao(db: AppDatabase): SyncBackupLogDao = db.syncBackupLogDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    fun provideWhoisQueryLogDao(db: AppDatabase): WhoisQueryLogDao = db.whoisQueryLogDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
