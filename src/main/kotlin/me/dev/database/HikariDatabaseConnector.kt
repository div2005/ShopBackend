package me.dev.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.dev.configuration.element.DatabaseConfig
import org.flywaydb.core.Flyway

import org.jetbrains.exposed.v1.jdbc.Database

class HikariDatabaseConnector : DatabaseConnector {
    override fun connect(config: DatabaseConfig) {
        val hikariConfig = HikariConfig()
        hikariConfig.driverClassName = config.driverClass
        hikariConfig.jdbcUrl = config.url
        hikariConfig.username = config.user
        hikariConfig.password = config.password
        hikariConfig.maximumPoolSize = config.maxPoolSize
        hikariConfig.isAutoCommit = false
        hikariConfig.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        hikariConfig.validate()

        val hikariDataSource = HikariDataSource(hikariConfig)

        Database.connect(HikariDataSource(hikariConfig))

        Flyway.configure()
            .baselineOnMigrate(true)
            .dataSource(hikariDataSource)
            .locations("classpath:db/migration")
            .validateMigrationNaming(true)
            .load()
            .migrate()
    }

    override fun close() {
    }
}