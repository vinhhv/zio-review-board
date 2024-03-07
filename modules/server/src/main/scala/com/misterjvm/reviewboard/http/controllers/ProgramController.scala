package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.domain.data.UserID
import com.misterjvm.reviewboard.http.endpoints.ProgramEndpoints
import com.misterjvm.reviewboard.services.{JWTService, ProgramService}
import sttp.tapir.server.ServerEndpoint
import zio.*

import scala.collection.mutable

class ProgramController private (service: ProgramService, jwtService: JWTService)
    extends BaseController
    with ProgramEndpoints {
  // create
  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { _ => request =>
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

  val allFilters: ServerEndpoint[Any, Task] =
    allFiltersEndpoint.serverLogic { _ =>
      service.allFilters.either
    }

  val search: ServerEndpoint[Any, Task] =
    searchEndpoint.serverLogic { filter =>
      service.search(filter).either
    }

  // ORDER MATTERS (allFilters must be BEFORE getById, otherwise it will always match getById)
  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, allFilters, search, getById)
}

object ProgramController {
  val makeZIO =
    for {
      service    <- ZIO.service[ProgramService]
      jwtService <- ZIO.service[JWTService]
    } yield new ProgramController(service, jwtService)
}
