package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.domain.data.{PaymentType, Program, User, UserID, UserToken}
import com.misterjvm.reviewboard.http.requests.CreateProgramRequest
import com.misterjvm.reviewboard.services.{JWTService, ProgramService}
import com.misterjvm.reviewboard.syntax.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

object ProgramControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val pjf =
    Program(1, "pjf-performance", "PJF Performance", "pjf.com", 1, PaymentType.LifetimeAccess)

  private val serviceStub = new ProgramService {
    override def create(request: CreateProgramRequest): Task[Program] =
      ZIO.succeed(request.toProgram(1L))

    override def getAll: Task[List[Program]] =
      ZIO.succeed(List(pjf))

    override def getById(id: Long): Task[Option[Program]] =
      ZIO.succeed {
        if (id == 1L) Some(pjf)
        else None
      }

    override def getBySlug(slug: String): Task[Option[Program]] =
      ZIO.succeed {
        if (slug == pjf.slug) Some(pjf)
        else None
      }

  }

  private val jwtServiceStub = new JWTService {
    val TOKEN = "TOKEN"
    override def createToken(user: User): Task[UserToken] =
      ZIO.succeed(UserToken(user.email, TOKEN, 999999999L))

    override def verifyToken(token: String): Task[UserID] =
      if (token == TOKEN)
        ZIO.succeed(UserID(1, "vinh@misterjvm.com"))
      else
        ZIO.fail(new RuntimeException(s"Invalid user token $token"))
  }

  private def backendStubZIO(endpointF: ProgramController => ServerEndpoint[Any, Task]) =
    for {
      // create the controller
      controller <- ProgramController.makeZIO
      // build the tapir backend
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointRunLogic(endpointF(controller))
          .backend()
      )
    } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ProgramControllerSpec")(
      test("POST /programs") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/programs")
            .body(
              CreateProgramRequest("PJF Performance", "pjf.com", 1, PaymentType.LifetimeAccess).toJson
            )
            .header("Authorization", "Bearer TOKEN")
            .send(backendStub)
        } yield response.body
        // inspect http response
        program.assert("creates a new Program") { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[Program].toOption)
            .contains(
              Program(1, "pjf-performance", "PJF Performance", "pjf.com", 1, PaymentType.LifetimeAccess)
            )
        }
      },
      test("POST /programs should fail if invalid token") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/programs")
            .body(
              CreateProgramRequest("PJF Performance", "pjf.com", 1, PaymentType.LifetimeAccess).toJson
            )
            .header("Authorization", "Bearer BAD_TOKEN")
            .send(backendStub)
        } yield response.body
        program.assert("creates a new Program") { respBody => respBody.isLeft }
      },
      test("GET /programs") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/programs")
            .send(backendStub)
        } yield response.body
        program.assert("returns a list with 1 value") { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[List[Program]].toOption)
            .contains(List(pjf))
        }
      },
      test("GET /programs/:id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/programs/1")
            .send(backendStub)
        } yield response.body
        program.assert("returns Program with ID 1") { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[Program].toOption)
            .contains(pjf)
        }
      }
    ).provide(
      ZLayer.succeed(serviceStub),
      ZLayer.succeed(jwtServiceStub)
    )
}
