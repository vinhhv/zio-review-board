package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.domain.data.UserID
import com.misterjvm.reviewboard.http.endpoints.ReviewEndpoints
import com.misterjvm.reviewboard.services.{JWTService, ReviewService}
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (service: ReviewService, jwtService: JWTService)
    extends BaseController
    with ReviewEndpoints {
  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
      .serverLogic(userId => request => service.create(request, userId.id).either)

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic(id => service.getById(id).either)

  val getByProgramId: ServerEndpoint[Any, Task] =
    getByProgramIdEndpoint.serverLogic(programId => service.getByProgramId(programId).either)

  val getByProgramSlug: ServerEndpoint[Any, Task] =
    getByProgramSlugEndpoint.serverLogic(programSlug => service.getByProgramSlug(programSlug).either)

  val getSummary: ServerEndpoint[Any, Task] =
    getSummaryEndpoint.serverLogic(programId => service.getSummary(programId).either)

  val makeSummary: ServerEndpoint[Any, Task] =
    makeSummaryEndpoint.serverLogic(programId => service.makeSummary(programId).either)

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(getSummary, makeSummary, create, getById, getByProgramSlug, getByProgramId)
}

object ReviewController {
  val makeZIO =
    for {
      reviewService <- ZIO.service[ReviewService]
      jwtService    <- ZIO.service[JWTService]
    } yield new ReviewController(reviewService, jwtService)
}
