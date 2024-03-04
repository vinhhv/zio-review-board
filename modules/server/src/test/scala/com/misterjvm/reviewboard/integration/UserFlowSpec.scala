package com.misterjvm.reviewboard.integration

import com.misterjvm.reviewboard.config.JWTConfig
import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.http.controllers.*
import com.misterjvm.reviewboard.http.endpoints.EndpointConstants
import com.misterjvm.reviewboard.http.requests.*
import com.misterjvm.reviewboard.http.responses.*
import com.misterjvm.reviewboard.repositories.{Repository, RepositorySpec, UserRepository, UserRepositoryLive}
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
  // http controller
  // service
  // repository
  // test container

  private val EMAIL    = "vinh@misterjvm.com"
  private val PASSWORD = "misterjvm"
  private val USERS    = s"/${USERS_ENDPOINT}"

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

    def put[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, None)

    def putAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, Some(token))

    def delete[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, None)

    def deleteAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, Some(token))
  }

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
        val NEW_PASSWORD = "scalarulez"
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
      }
    ).provide(
      UserServiceLive.layer,
      JWTServiceLive.layer,
      UserRepositoryLive.layer,
      Repository.quillLayer,
      dataSourceLayer,
      ZLayer.succeed(JWTConfig("secret", 3600)),
      Scope.default
    )
}
