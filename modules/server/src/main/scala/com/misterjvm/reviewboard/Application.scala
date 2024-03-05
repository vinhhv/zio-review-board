package com.misterjvm.reviewboard

import com.misterjvm.reviewboard.config.{Configs, JWTConfig}
import com.misterjvm.reviewboard.http.HttpAPI
import com.misterjvm.reviewboard.http.controllers.{HealthController, ProgramController}
import com.misterjvm.reviewboard.repositories.{
  ProgramRepositoryLive,
  RecoveryTokensRepositoryLive,
  Repository,
  ReviewRepositoryLive,
  UserRepositoryLive
}
import com.misterjvm.reviewboard.services.{
  EmailServiceLive,
  JWTServiceLive,
  ProgramServiceLive,
  ReviewServiceLive,
  UserServiceLive
}
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
  } yield ()

  override def run =
    serverProgram.provide(
      Server.default,
      // services
      ProgramServiceLive.layer,
      ReviewServiceLive.layer,
      UserServiceLive.layer,
      JWTServiceLive.configuredLayer,
      EmailServiceLive.configuredLayer,
      // repos
      ProgramRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokensRepositoryLive.configuredLayer,
      // other requirements
      Repository.dataLayer
    )
}
