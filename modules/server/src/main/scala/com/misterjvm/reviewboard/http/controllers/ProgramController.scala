package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.http.endpoints.ProgramEndpoints
import sttp.tapir.server.ServerEndpoint
import zio.*

import scala.collection.mutable

class ProgramController extends BaseController with ProgramEndpoints {
  // TODO implementations
  // in-memory "database"
  val db = mutable.Map[Long, Program]()

  // create
  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { request =>
    ZIO.succeed {
      val newId      = db.keys.maxOption.getOrElse(0L) + 1
      val newProgram = request.toProgram(newId)
      db += (newId -> newProgram)
      newProgram
    }
  }

  // getAll
  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogicSuccess(_ => ZIO.succeed(db.values.toList))

  // getById
  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogicSuccess { id =>
    ZIO
      .attempt(id.toLong)
      .map(db.get)
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object ProgramController {
  val makeZIO = ZIO.succeed(new ProgramController)
}
