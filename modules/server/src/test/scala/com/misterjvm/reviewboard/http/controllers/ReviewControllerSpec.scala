package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.domain.data.{DataFixtures, MetricScore, Review}
import com.misterjvm.reviewboard.http.requests.CreateReviewRequest
import com.misterjvm.reviewboard.services.ReviewService
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

object ReviewControllerSpec extends ZIOSpecDefault with DataFixtures {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val serviceStub = new ReviewService {
    override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
      ZIO.succeed(goodReview)

    override def getById(id: Long): Task[Option[Review]] =
      ZIO.succeed {
        if (id == 1) Some(goodReview)
        else None
      }

    override def getByProgramId(programId: Long): Task[List[Review]] =
      ZIO.succeed {
        if (programId == 1) List(goodReview)
        else List()
      }

    override def getByUserId(userId: Long): Task[List[Review]] =
      ZIO.succeed {
        if (userId == 1) List(goodReview)
        else List()
      }
  }

  private def backendStubZIO(endpointF: ReviewController => ServerEndpoint[Any, Task]) =
    for {
      controller <- ReviewController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointRunLogic(endpointF(controller))
          .backend()
      )
    } yield backendStub

  import MetricScore.*

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewControllerSpec")(
      test("create review") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"reviews")
            .body(
              CreateReviewRequest(
                programId = 1L,
                value = Amazing,
                quality = Amazing,
                content = Amazing,
                userExperience = Amazing,
                accessibility = Amazing,
                support = Amazing,
                wouldRecommend = Amazing,
                review = "Wow!"
              ).toJson
            )
            .send(backendStub)
        } yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(goodReview)
        )
      },
      test("getById") {
        for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"reviews/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"reviews/999")
            .send(backendStub)
        } yield assertTrue(
          response.body.toOption.flatMap(_.fromJson[Review].toOption).contains(goodReview) &&
            responseNotFound.body.toOption.flatMap(_.fromJson[Review].toOption).isEmpty
        )
      },
      test("getByProgramId") {
        for {
          backendStub <- backendStubZIO(_.getByProgramId)
          response <- basicRequest
            .get(uri"reviews/program/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"reviews/program/999")
            .send(backendStub)
        } yield assertTrue(
          response.body.toOption.flatMap(_.fromJson[List[Review]].toOption).contains(List(goodReview)) &&
            responseNotFound.body.toOption.flatMap(_.fromJson[List[Review]].toOption).contains(List())
        )
      }
    ).provide(ZLayer.succeed(serviceStub))
}