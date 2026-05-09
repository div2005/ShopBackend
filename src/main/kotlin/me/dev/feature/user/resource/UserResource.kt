package me.dev.feature.user.resource

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.resources.patch
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.dev.feature.user.domain.UserRepository
import me.dev.feature.user.domain.model.*
import org.koin.ktor.ext.inject

@Resource("/users")
class Users {
    @Resource("{id}")
    class Id(val parent: Users, val id: String)
}

@Resource("/session")
class Session {
    @Resource("login")
    class Login(val parent: Session)

    @Resource("logout")
    class Logout(val parent: Session)

    @Resource("token")
    class Token(val parent: Session)
}

fun Route.userEndpoint() {
    val userRepository by inject<UserRepository>()

    post<Session.Login> {
        val request = call.receive<UserLoginRequest>()
        try {
            val result = userRepository.loginUser(request)
            if (result != null) call.respond(HttpStatusCode.OK, result)
            else call.respond(HttpStatusCode.Unauthorized)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    authenticate("token") {
        get<Users> {
            val result = userRepository.getAllUsers()
            call.respond(result)
        }

        get<Users.Id> {
            val result = userRepository.getUser(it.id)
            if (result != null) call.respond(HttpStatusCode.OK, result)
            else call.respond(HttpStatusCode.NotFound)
        }

        post<Users> {
            val request = call.receive<UserCreateRequest>()
            try {
                val result = userRepository.createUser(request)

                if (
                    result.status == UserCreateResponse.StatusType.USERNAME_ALREADY_IN_USE ||
                    result.status == UserCreateResponse.StatusType.EMAIL_ALREADY_IN_USE
                    )
                    call.respond(HttpStatusCode.BadRequest, result.status)

                else if (result.status != null) call.respond(HttpStatusCode.BadRequest, result.status)
                else if (result.userDTO != null) call.respond(HttpStatusCode.OK, result)

                else call.respond(HttpStatusCode.NotAcceptable)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        patch<Users.Id> {
            val request = call.receive<UserUpdateRequest>()
            try {
                val result = userRepository.updateUser(it.id, request)

                if (
                    result.status == UserUpdateResponse.StatusType.USERNAME_ALREADY_IN_USE ||
                    result.status == UserUpdateResponse.StatusType.EMAIL_ALREADY_IN_USE
                )
                    call.respond(HttpStatusCode.BadRequest, result.status)

                else if (result.status == UserUpdateResponse.StatusType.NOT_FOUND)
                    call.respond(HttpStatusCode.NotFound, result.status)

                else if (result.status != null) call.respond(HttpStatusCode.BadRequest, result.status)
                else if (result.userDTO != null) call.respond(HttpStatusCode.OK, result)

                else call.respond(HttpStatusCode.NotAcceptable)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        delete<Users.Id> {
            try {
                val result = userRepository.removeUser(it.id)

                if (result != null) call.respond(HttpStatusCode.OK, result)

                else call.respond(HttpStatusCode.BadRequest)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post<Session.Logout> {
            try {
                val token = call.request.authorization()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val result = userRepository.logoutUser(token)

                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get<Session.Token> {
            try {
                val token = call.request.authorization() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val rawToken = if (token.startsWith("Bearer ")) token.substring(7) else token
                val result = userRepository.getTokenValidity(rawToken)

                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}