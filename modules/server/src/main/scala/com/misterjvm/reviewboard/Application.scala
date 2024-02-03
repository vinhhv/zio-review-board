package com.misterjvm.reviewboard

import com.misterjvm.reviewboard.http.HttpAPI
import com.misterjvm.reviewboard.http.controllers.{HealthController, ProgramController}
import com.misterjvm.reviewboard.repositories.{ProgramRepositoryLive, Repository}
import com.misterjvm.reviewboard.services.ProgramServiceLive
import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints <- HttpAPI.endpointsZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(endpoints)
    )
    _ <- Console.printLine("Mister JVM!")
  } yield ()

  override def run =
    serverProgram.provide(
      Server.default,
      // services
      ProgramServiceLive.layer,
      // repos
      ProgramRepositoryLive.layer,
      // other requirements
      Repository.dataLayer
    )
}
