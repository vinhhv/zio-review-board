package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.domain.errors.HttpError
import com.misterjvm.reviewboard.http.endpoints.HealthEndpoint
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*

class HealthController private extends BaseController with HealthEndpoint {
  val health: ServerEndpoint[Any, Task] =
    healthEndpoint
      .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  val errorRoute =
    errorEndpoint
      .serverLogic[Task](_ => ZIO.fail(new RuntimeException("Boom!")).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(health, errorRoute)
}

object HealthController {
  val makeZIO = ZIO.succeed(new HealthController)
}
