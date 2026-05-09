package me.dev.plugins

import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import me.dev.common.seed.SeedService
import org.koin.ktor.ext.inject
import me.dev.configuration.element.KtorConfig
import me.dev.database.DatabaseConnector
import me.dev.feature.item.data.ItemTable
import me.dev.feature.order.data.OrderItemTable
import me.dev.feature.order.data.OrderTable
import me.dev.feature.user.data.UserTable
import me.dev.feature.user.data.UserTokensTable
import org.jetbrains.exposed.v1.core.exposedLogger
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

fun Application.configureDatabase(){
    val ktorConfig by inject<KtorConfig>()
    val databaseProvider by inject<DatabaseConnector>()
    val seedService by inject<SeedService>()

    databaseProvider.connect(ktorConfig.databaseConfig)

    if (ktorConfig.development) {
        transaction {
            val changes = MigrationUtils.statementsRequiredForDatabaseMigration(
                ItemTable,
                OrderTable,
                OrderItemTable,
                UserTable,
                UserTokensTable
            ).joinToString("\n")
            exposedLogger.info(if (changes.isNotBlank()) "Database schema needs to be updated with the following changes:\n$changes" else "Database schema is up to date")
        }
    }

    runBlocking { seedService.seed() }
}