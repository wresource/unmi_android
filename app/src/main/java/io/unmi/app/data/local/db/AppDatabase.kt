package io.unmi.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.unmi.app.data.local.db.dao.*
import io.unmi.app.data.local.db.entity.*

@Database(
    entities = [
        AccountEntity::class,
        DomainEntity::class,
        RenewalRecordEntity::class,
        SyncBackupLogEntity::class,
        TagEntity::class,
        DomainTagCrossRef::class,
        WhoisQueryLogEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun domainDao(): DomainDao
    abstract fun renewalRecordDao(): RenewalRecordDao
    abstract fun syncBackupLogDao(): SyncBackupLogDao
    abstract fun tagDao(): TagDao
    abstract fun whoisQueryLogDao(): WhoisQueryLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE domains ADD COLUMN estimatedValue REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE domains ADD COLUMN valueCurrency TEXT NOT NULL DEFAULT 'USD'")
                db.execSQL("ALTER TABLE domains ADD COLUMN valueGrade TEXT")
                db.execSQL("ALTER TABLE domains ADD COLUMN valueConfidence TEXT")
                db.execSQL("ALTER TABLE domains ADD COLUMN nameservers TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN isGuest INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create accounts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        displayName TEXT,
                        passwordHash TEXT NOT NULL,
                        passwordSalt TEXT NOT NULL,
                        createdAt TEXT NOT NULL,
                        lastLoginAt TEXT
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_passwordHash ON accounts (passwordHash)")

                // Create default account for existing data
                db.execSQL("""
                    INSERT INTO accounts (id, displayName, passwordHash, passwordSalt, createdAt)
                    VALUES (1, '默认账户', '', '', datetime('now'))
                """)

                // Add accountId to domains
                db.execSQL("ALTER TABLE domains ADD COLUMN accountId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_domains_accountId ON domains (accountId)")

                // Recreate unique index for domainName+accountId
                db.execSQL("DROP INDEX IF EXISTS index_domains_domainName")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_domains_domainName_accountId ON domains (domainName, accountId)")
            }
        }
    }
}
