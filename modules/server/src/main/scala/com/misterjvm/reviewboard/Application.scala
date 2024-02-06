package com.misterjvm.reviewboard

import com.misterjvm.reviewboard.http.HttpAPI
import com.misterjvm.reviewboard.http.controllers.{HealthController, ProgramController}
import com.misterjvm.reviewboard.repositories.{ProgramRepositoryLive, Repository, ReviewRepositoryLive}
import com.misterjvm.reviewboard.services.{ProgramServiceLive, ReviewServiceLive}
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
      ReviewServiceLive.layer,
      // repos
      ProgramRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      // other requirements
      Repository.dataLayer
    )
}
