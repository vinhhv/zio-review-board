package com.misterjvm.reviewboard

import com.misterjvm.reviewboard.config.{Configs, JWTConfig}
import com.misterjvm.reviewboard.http.HttpAPI
import com.misterjvm.reviewboard.http.controllers.{HealthController, ProgramController}
import com.misterjvm.reviewboard.repositories.{
  ProgramRepositoryLive,
  Repository,
  ReviewRepositoryLive,
  UserRepositoryLive
}
import com.misterjvm.reviewboard.services.{JWTServiceLive, ProgramServiceLive, ReviewServiceLive, UserServiceLive}
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
      // configs
      Configs.makeLayer[JWTConfig]("misterjvm.jwt"),
      // services
      ProgramServiceLive.layer,
      ReviewServiceLive.layer,
      UserServiceLive.layer,
      JWTServiceLive.layer,
      // repos
      ProgramRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      // other requirements
      Repository.dataLayer
    )
}
