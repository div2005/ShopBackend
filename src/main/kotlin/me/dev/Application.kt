package me.dev

import io.ktor.server.application.*
import org.koin.core.module.Module
import me.dev.plugins.configureKoin
import me.dev.configuration.setupConfig
import me.dev.dependencyInjection.appModule
import me.dev.plugins.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module(testing: Boolean = false, koinModules: List<Module> = listOf(appModule)) {
    configureKoin(koinModules)
    setupConfig()
    configureDatabase()
    configureSecurity()
    configureHTTP()
    configureSerialization()
    configureRouting()
}
