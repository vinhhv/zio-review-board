package com.misterjvm.reviewboard.integration

import com.misterjvm.reviewboard.config.{JWTConfig, RecoveryTokensConfig}
import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.http.controllers.*
import com.misterjvm.reviewboard.http.endpoints.EndpointConstants
import com.misterjvm.reviewboard.http.requests.*
import com.misterjvm.reviewboard.http.responses.*
import com.misterjvm.reviewboard.repositories.{
  RecoveryTokensRepositoryLive,
  Repository,
  RepositorySpec,
  UserRepository,
  UserRepositoryLive
}
import com.misterjvm.reviewboard.services.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{SttpBackend, _}
import sttp.model.Method
import sttp.monad.MonadError
import sttp.tapir.generic.auto.*
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

object UserFlowSpec extends ZIOSpecDefault with RepositorySpec with EndpointConstants {

  private val EMAIL        = "vinh@misterjvm.com"
  private val PASSWORD     = "misterjvm"
  private val NEW_PASSWORD = "scalarulez"
  private val USERS        = s"/${USERS_ENDPOINT}"

  override val initScript: String = "sql/integration.sql"

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private def backendStubZIO =
    for {
      controller <- UserController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointsRunLogic(controller.routes)
          .backend()
      )
    } yield backendStub

  extension [A: JsonCodec](backend: SttpBackend[Task, Nothing]) {
    def sendRequest[B: JsonCodec](
        method: Method,
        path: String,
        payload: A,
        maybeToken: Option[String] = None
    ): Task[Option[B]] =
      basicRequest
        .method(method, uri"$path")
        .body(payload.toJson)
        .auth
        .bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body)
        .map(_.toOption.flatMap(payload => payload.fromJson[B].toOption))

    def post[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, None)

    def postAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, Some(token))

    def postNoResponse(path: String, payload: A): Task[Unit] =
      basicRequest
        .method(Method.POST, uri"$path")
        .body(payload.toJson)
        .send(backend)
        .unit

    def put[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, None)

    def putAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, Some(token))

    def delete[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, None)

    def deleteAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, Some(token))
  }

  class EmailServiceProbe extends EmailService {
    val db = collection.mutable.Map[String, String]()
    override def sendEmail(to: String, subject: String, content: String): Task[Unit] =
      ZIO.unit
    override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] =
      ZIO.succeed(db += (to -> token))
    def probeToken(email: String): Task[Option[String]] = ZIO.succeed(db.get(email))
  }

  val emailServiceLayer: ZLayer[Any, Nothing, EmailServiceProbe] = ZLayer.succeed(new EmailServiceProbe)

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserFlowSpec")(
      test("create user") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse](USERS, RegisterUserAccount(EMAIL, PASSWORD))
        } yield assertTrue(maybeResponse.contains(UserResponse(EMAIL)))
      },
      test("create and log in") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse](USERS, RegisterUserAccount(EMAIL, PASSWORD))
          maybeToken    <- backendStub.post[UserToken](s"$USERS/login", LoginRequest(EMAIL, PASSWORD))
        } yield assertTrue(
          maybeToken.filter(_.email == EMAIL).nonEmpty
        )
      },
      test("change password") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse](USERS, RegisterUserAccount(EMAIL, PASSWORD))
          userToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest(EMAIL, PASSWORD))
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub
            .putAuth[UserResponse](
              "/users/password",
              UpdatePasswordRequest(EMAIL, PASSWORD, NEW_PASSWORD),
              userToken.token
            )
          maybeOldToken <- backendStub.post[UserToken](s"$USERS/login", LoginRequest(EMAIL, PASSWORD))
          maybeNewToken <- backendStub.post[UserToken](s"$USERS/login", LoginRequest(EMAIL, NEW_PASSWORD))
        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.nonEmpty
        )
      },
      test("delete user") {
        for {
          backendStub   <- backendStubZIO
          userRepo      <- ZIO.service[UserRepository]
          maybeResponse <- backendStub.post[UserResponse](USERS, RegisterUserAccount(EMAIL, PASSWORD))
          maybeOldUser  <- userRepo.getByEmail(EMAIL)
          userToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest(EMAIL, PASSWORD))
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub
            .deleteAuth[UserResponse](
              "/users",
              DeleteAccountRequest(EMAIL, PASSWORD),
              userToken.token
            )
          maybeUser <- userRepo.getByEmail(EMAIL)
        } yield assertTrue(
          maybeOldUser.filter(_.email == EMAIL).nonEmpty && maybeUser.isEmpty
        )
      },
      test("recover password flow")(
        for {
          backendStub       <- backendStubZIO
          _                 <- backendStub.post[UserResponse](USERS, RegisterUserAccount(EMAIL, PASSWORD))
          _                 <- backendStub.postNoResponse(s"$USERS/forgot", ForgotPasswordRequest(EMAIL))
          emailServiceProbe <- ZIO.service[EmailServiceProbe]
          token <-
            emailServiceProbe
              .probeToken(EMAIL)
              .someOrFail(new RuntimeException("Token was NOT emailed!"))
          _ <- backendStub.postNoResponse(s"$USERS/recover", RecoverPasswordRequest(EMAIL, token, "scalarulez"))
          maybeOldToken <- backendStub.post[UserToken](s"$USERS/login", LoginRequest(EMAIL, PASSWORD))
          maybeNewToken <- backendStub.post[UserToken](s"$USERS/login", LoginRequest(EMAIL, NEW_PASSWORD))
        } yield assertTrue(maybeOldToken.isEmpty && maybeNewToken.nonEmpty)
      )
    ).provide(
      UserServiceLive.layer,
      JWTServiceLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokensRepositoryLive.layer,
      emailServiceLayer,
      Repository.quillLayer,
      dataSourceLayer,
      ZLayer.succeed(JWTConfig("secret", 3600)),
      ZLayer.succeed(RecoveryTokensConfig(24 * 3600)),
      Scope.default
    )
}
