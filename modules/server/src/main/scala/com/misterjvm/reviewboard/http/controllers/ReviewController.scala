package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.http.endpoints.ReviewEndpoints
import com.misterjvm.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.*
import com.misterjvm.reviewboard.services.JWTService
import com.misterjvm.reviewboard.domain.data.UserID

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

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(create, getById, getByProgramId)
}

object ReviewController {
  val makeZIO =
    for {
      reviewService <- ZIO.service[ReviewService]
      jwtService    <- ZIO.service[JWTService]
    } yield new ReviewController(reviewService, jwtService)
}
