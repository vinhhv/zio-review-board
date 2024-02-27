package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.http.endpoints.ProgramEndpoints
import com.misterjvm.reviewboard.services.ProgramService
import sttp.tapir.server.ServerEndpoint
import zio.*

import scala.collection.mutable

class ProgramController private (service: ProgramService) extends BaseController with ProgramEndpoints {
  // create
  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogic { request =>
    service.create(request).either
  }

  // getAll
  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogic { _ =>
    service.getAll.either
  }

  // getById
  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogic { id =>
    ZIO
      .attempt(id.toLong)
      .flatMap(service.getById)
      .catchSome { case _: java.lang.NumberFormatException =>
        service.getBySlug(id)
      }
      .either
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object ProgramController {
  val makeZIO =
    for {
      service <- ZIO.service[ProgramService]
    } yield new ProgramController(service)
}
