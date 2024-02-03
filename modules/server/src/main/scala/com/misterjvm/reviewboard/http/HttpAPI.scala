package com.misterjvm.reviewboard.http

import com.misterjvm.reviewboard.http.controllers.*

object HttpAPI {
  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  def makeControllers = for {
    health   <- HealthController.makeZIO
    programs <- ProgramController.makeZIO
  } yield List(health, programs)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}
