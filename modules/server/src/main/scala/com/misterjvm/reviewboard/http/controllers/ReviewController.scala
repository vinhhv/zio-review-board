package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.http.endpoints.ReviewEndpoints
import com.misterjvm.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (service: ReviewService) extends BaseController with ReviewEndpoints {
  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogicSuccess(request => service.create(request, -1L /*TODO: add user id*/ ))

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess(id => service.getById(id))

  val getByProgramId: ServerEndpoint[Any, Task] =
    getByProgramIdEndpoint.serverLogicSuccess(programId => service.getByProgramId(programId))

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(create, getById, getByProgramId)
}

object ReviewController {
  val makeZIO = ZIO.service[ReviewService].map(service => new ReviewController(service))
}
