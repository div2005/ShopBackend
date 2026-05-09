package me.dev.database

import me.dev.configuration.element.DatabaseConfig

interface DatabaseConnector {
    fun connect(config: DatabaseConfig)
    fun close()
}