package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.domain.data.UserID
import com.misterjvm.reviewboard.domain.errors.*
import com.misterjvm.reviewboard.http.endpoints.UserEndpoints
import com.misterjvm.reviewboard.http.responses.UserResponse
import com.misterjvm.reviewboard.services.{JWTService, UserService}
import sttp.tapir.*
import sttp.tapir.server.*
import zio.*

class UserController private (userService: UserService, jwtService: JWTService)
    extends BaseController
    with UserEndpoints {

  val create: ServerEndpoint[Any, Task] = createUserEndpoint
    .serverLogic { req =>
      userService
        .registerUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  val login: ServerEndpoint[Any, Task] = loginEndpoint
    .serverLogic { req =>
      userService
        .generateToken(req.email, req.password)
        .someOrFail(UnauthorizedException)
        .either
    }

  // change password (check for JWT)
  val updatePassword: ServerEndpoint[Any, Task] = updatePasswordEndpoint
    .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { userId => req =>
      userService
        .updatePassword(req.email, req.oldPassword, req.newPassword)
        .map(user => UserResponse(user.email))
        .either
    }

  // delete account
  val delete: ServerEndpoint[Any, Task] = deleteEndpoint
    .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { userId => req =>
      userService
        .deleteUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(
      create,
      updatePassword,
      delete,
      login
    )
}

object UserController {
  val makeZIO = for {
    userService <- ZIO.service[UserService]
    jwtService  <- ZIO.service[JWTService]
  } yield UserController(userService, jwtService)
}
