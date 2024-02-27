package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.http.endpoints.ReviewEndpoints
import com.misterjvm.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (service: ReviewService) extends BaseController with ReviewEndpoints {
  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogic(request => service.create(request, -1L /*TODO: add user id*/ ).either)

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic(id => service.getById(id).either)

  val getByProgramId: ServerEndpoint[Any, Task] =
    getByProgramIdEndpoint.serverLogic(programId => service.getByProgramId(programId).either)

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(create, getById, getByProgramId)
}

object ReviewController {
  val makeZIO = ZIO.service[ReviewService].map(service => new ReviewController(service))
}
