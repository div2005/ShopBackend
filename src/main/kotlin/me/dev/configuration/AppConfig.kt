package me.dev.configuration

import io.ktor.server.application.*
import org.koin.ktor.ext.inject
import me.dev.configuration.element.*

fun Application.setupConfig(){
    val ktorConfig by inject<KtorConfig>()

    setupKtor(ktorConfig)
    setupDatabase(ktorConfig)
}

private fun Application.setupKtor(ktorConfig: KtorConfig) {
    val ktorObject = environment.config.config("ktor")
    val development = ktorObject.property("development").getString().toBoolean()
    ktorConfig.development = development;

    val deploymentObject = environment.config.config("ktor.deployment")
    val port = deploymentObject.property("port").getString().toInt()
    ktorConfig.deploymentConfig = DeploymentConfig(port)
}

private fun Application.setupDatabase(ktorConfig: KtorConfig) {
    ktorConfig.databaseConfig = environment.config.config("database").run {
        val driverClass = property("driverClass").getString()
        val url = property("url").getString()
        val user = property("user").getString()
        val password = property("password").getString()
        val maxPoolSize = property("maxPoolSize").getString().toInt()
        DatabaseConfig(driverClass, url, user, password, maxPoolSize)
    }
}