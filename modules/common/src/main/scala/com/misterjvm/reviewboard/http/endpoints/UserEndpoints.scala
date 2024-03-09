package com.misterjvm.reviewboard.http.endpoints

import com.misterjvm.reviewboard.domain.data.UserToken
import com.misterjvm.reviewboard.http.requests.*
import com.misterjvm.reviewboard.http.responses.UserResponse
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait UserEndpoints extends BaseEndpoint with EndpointConstants {

  // POST /users { email, password } -> { email }
  val createUserEndpoint =
    baseEndpoint
      .tag("Users")
      .name("register")
      .description("Register a user with username and password")
      .in(USERS_ENDPOINT)
      .post
      .in(jsonBody[RegisterUserAccount])
      .out(jsonBody[UserResponse])

  // PUT /users { email, oldPassword, newPassword } -> { email }
  val updatePasswordEndpoint =
    secureBaseEndpoint
      .tag("Users")
      .name("update password")
      .description("Update user password")
      .in(USERS_ENDPOINT / "password")
      .post
      .in(jsonBody[UpdatePasswordRequest])
      .out(jsonBody[UserResponse])

  // DELETE /users { email, password } -> { email }
  val deleteEndpoint =
    secureBaseEndpoint
      .tag("Users")
      .name("delete")
      .description("Delete user account")
      .in(USERS_ENDPOINT)
      .delete
      .in(jsonBody[DeleteAccountRequest])
      .out(jsonBody[UserResponse])

  // POST /users/login { email, password } -> { email, accessToken, expiration }
  val loginEndpoint =
    baseEndpoint
      .tag("Users")
      .name("login")
      .description("Log in and generate a JWT token")
      .in(USERS_ENDPOINT / "login")
      .post
      .in(jsonBody[LoginRequest])
      .out(jsonBody[UserToken])

  // POST /users/forgot { email } - 200 OK
  val forgotPasswordEndpoint =
    baseEndpoint
      .tag("Users")
      .name("forgot password")
      .description("Trigger email for password recovery")
      .in(USERS_ENDPOINT / "forgot")
      .post
      .in(jsonBody[ForgotPasswordRequest])

  // POST /users/recover { email, token, newPassword }
  val recoverPasswordEndpoint =
    baseEndpoint
      .tag("Users")
      .name("recover password")
      .description("Set new password based on OTP")
      .in(USERS_ENDPOINT / "recover")
      .post
      .in(jsonBody[RecoverPasswordRequest])
}
