package me.dev.common.password

import at.favre.lib.crypto.bcrypt.BCrypt
import me.dev.feature.user.data.User
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID

class PasswordServiceImpl: PasswordService {

    override fun validatePassword(password: String): Boolean {
        val pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=])(?=\\S+\$).{8,24}\$"
        return password.matches(Regex(pattern))
    }
    override fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    override fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }

    override suspend fun verifyUserPassword(uuid: String, password: String): Boolean {
        val hash = suspendTransaction {
            return@suspendTransaction User.findById(UUID.fromString(uuid))?.password
        } ?: return false

        return verifyPassword(password, hash)
    }
}