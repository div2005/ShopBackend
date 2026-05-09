package me.dev.feature.user.domain

import kotlinx.datetime.toKotlinLocalDateTime
import me.dev.common.password.PasswordService
import me.dev.common.token.TokenService
import me.dev.common.username.UserDataValidatorService
import me.dev.feature.user.data.User
import me.dev.feature.user.data.UserTable
import me.dev.feature.user.data.UserTokens
import me.dev.feature.user.data.UserTokensTable
import me.dev.feature.user.domain.mapper.toDTO
import me.dev.feature.user.domain.model.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.component.KoinComponent
import java.time.LocalDateTime
import java.util.*

class UserRepositoryImpl(
    private val passwordService: PasswordService,
    private val userDataValidatorService: UserDataValidatorService,
    private val tokenService: TokenService
): UserRepository, KoinComponent {


    override suspend fun getAllUsers(): List<UserDTO> {
        val users = suspendTransaction {
            return@suspendTransaction User.all().sortedBy { i -> i.firstName }.toList()
        }
        return users.map { u -> u.toDTO() }
    }

    override suspend fun getUser(uuid: String): UserDTO? {
        return suspendTransaction {
            return@suspendTransaction User.findById(UUID.fromString(uuid))?.toDTO()
        }
    }

    override suspend fun createUser(request: UserCreateRequest): UserCreateResponse {
        return suspendTransaction {

            if (!User.find { UserTable.username.lowerCase() eq request.username.lowercase() }.empty())
                return@suspendTransaction UserCreateResponse(status = UserCreateResponse.StatusType.USERNAME_ALREADY_IN_USE)

            if (!User.find { UserTable.email.lowerCase() eq request.email.lowercase() }.empty())
                return@suspendTransaction UserCreateResponse(status = UserCreateResponse.StatusType.EMAIL_ALREADY_IN_USE)

            if (!passwordService.validatePassword(request.password))
                return@suspendTransaction UserCreateResponse(status = UserCreateResponse.StatusType.BAD_PASSWORD)

            if (!userDataValidatorService.validateUsername(request.username))
                return@suspendTransaction UserCreateResponse(status = UserCreateResponse.StatusType.BAD_USERNAME)

            if (!userDataValidatorService.validateFirstOrSecondName(request.firstName))
                return@suspendTransaction UserCreateResponse(status = UserCreateResponse.StatusType.BAD_NAME)

            if (!userDataValidatorService.validateFirstOrSecondName(request.secondName))
                return@suspendTransaction UserCreateResponse(status = UserCreateResponse.StatusType.BAD_NAME)

            if (!userDataValidatorService.validateEmail(request.email))
                return@suspendTransaction UserCreateResponse(status = UserCreateResponse.StatusType.BAD_MAIL)

            return@suspendTransaction UserCreateResponse(userDTO =
            User.new {
                firstName = request.firstName
                secondName = request.secondName
                email = request.email
                username = request.username
                password = passwordService.hashPassword(request.password)
            }.toDTO()
            )
        }
    }

    override suspend fun updateUser(uuid: String, request: UserUpdateRequest): UserUpdateResponse {
        return suspendTransaction {
            val user = User.findById(UUID.fromString(uuid))
                ?: return@suspendTransaction UserUpdateResponse(status = UserUpdateResponse.StatusType.NOT_FOUND)

            if (
                request.username != null &&
                request.username != user.username &&
                !User.find { UserTable.username.lowerCase() eq request.username.lowercase() }.empty()
                )
                return@suspendTransaction UserUpdateResponse(status = UserUpdateResponse.StatusType.USERNAME_ALREADY_IN_USE)

            if (
                request.email != null &&
                request.email != user.email &&
                !User.find { UserTable.email.lowerCase() eq request.email.lowercase() }.empty()
                )
                return@suspendTransaction UserUpdateResponse(status = UserUpdateResponse.StatusType.EMAIL_ALREADY_IN_USE)

            if (!request.password.isNullOrEmpty() && !passwordService.validatePassword(request.password))
                return@suspendTransaction UserUpdateResponse(status = UserUpdateResponse.StatusType.BAD_PASSWORD)

            if (!request.username.isNullOrEmpty() && !userDataValidatorService.validateUsername(request.username))
                return@suspendTransaction UserUpdateResponse(status = UserUpdateResponse.StatusType.BAD_USERNAME)

            if (!request.firstName.isNullOrEmpty() && !userDataValidatorService.validateFirstOrSecondName(request.firstName))
                return@suspendTransaction UserUpdateResponse(status = UserUpdateResponse.StatusType.BAD_NAME)

            if (!request.secondName.isNullOrEmpty() && !userDataValidatorService.validateFirstOrSecondName(request.secondName))
                return@suspendTransaction UserUpdateResponse(status = UserUpdateResponse.StatusType.BAD_NAME)

            if (!request.email.isNullOrEmpty() && !userDataValidatorService.validateEmail(request.email))
                return@suspendTransaction UserUpdateResponse(status = UserUpdateResponse.StatusType.BAD_MAIL)

            user.apply {
                if (!request.username.isNullOrEmpty()) username = request.username
                if (!request.firstName.isNullOrEmpty()) firstName = request.firstName
                if (!request.secondName.isNullOrEmpty()) secondName = request.secondName
                if (!request.email.isNullOrEmpty()) email = request.email
                if (!request.password.isNullOrEmpty()) password = passwordService.hashPassword(request.password)
            }

            if (!request.password.isNullOrEmpty())
                UserTokens.find { UserTokensTable.user eq user.id }.forEach { token -> token.delete() }

            return@suspendTransaction UserUpdateResponse(
                userDTO = user.toDTO()
            )
        }
    }

    override suspend fun removeUser(uuid: String): UserDTO? {
        return suspendTransaction {
            val _uuid = UUID.fromString(uuid)
            val dto = User.findById(_uuid)?.toDTO() ?: return@suspendTransaction null

            User.findById(_uuid)?.delete()

            return@suspendTransaction dto
        }
    }

    override suspend fun loginUser(request: UserLoginRequest): UserLoginResponse? {
        return suspendTransaction {
            val user = User.find {
                    (UserTable.username.lowerCase() eq request.login.lowercase())
                        .or(UserTable.email.lowerCase() eq request.login.lowercase())
            }.firstOrNull()

            if (user == null || !passwordService.verifyPassword(request.password, user.password))
                return@suspendTransaction null

            val token = tokenService.generateToken()

            UserTokens.new {
                this.token = token
                this.user = user
                this.validTime = LocalDateTime.now().plusDays(90)
            }

            return@suspendTransaction UserLoginResponse(token, user.toDTO())
        }
    }

    override suspend fun logoutUser(token: String) {
        return suspendTransaction {
            UserTokens.find { UserTokensTable.token eq token }.firstOrNull()?.delete()
        }
    }

    override suspend fun getTokenValidity(token: String): GetTokenValidityResponse {
        return suspendTransaction {
            val userToken = UserTokens.find { UserTokensTable.token eq token }.firstOrNull()

            return@suspendTransaction GetTokenValidityResponse(
                    userToken?.validTime?.toKotlinLocalDateTime() ?: LocalDateTime.MIN.toKotlinLocalDateTime(),
                    userToken?.user?.id?.toString()
            )
        }
    }
}
