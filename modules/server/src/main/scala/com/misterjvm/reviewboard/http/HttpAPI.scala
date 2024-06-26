package com.misterjvm.reviewboard.http

import com.misterjvm.reviewboard.http.controllers.*

object HttpAPI {
  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  def makeControllers = for {
    health   <- HealthController.makeZIO
    programs <- ProgramController.makeZIO
    reviews  <- ReviewController.makeZIO
    users    <- UserController.makeZIO
    invites  <- InviteController.makeZIO
  } yield List(health, programs, reviews, users, invites)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}
