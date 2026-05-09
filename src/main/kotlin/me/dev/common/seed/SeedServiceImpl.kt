package me.dev.common.seed

import me.dev.common.password.PasswordService
import me.dev.feature.user.data.User
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.component.KoinComponent

class SeedServiceImpl(
    private val passwordService: PasswordService
) : SeedService, KoinComponent {
    override suspend fun seed() {
        suspendTransaction {
            if (!User.all().empty()) return@suspendTransaction

            User.new {
                firstName = "Admin"
                secondName = "Root"
                email = "admin@pizzeria.com"
                username = "admin"
                password = passwordService.hashPassword("admin")
            }

        }
    }
}