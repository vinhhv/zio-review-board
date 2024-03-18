package com.misterjvm.reviewboard

import com.misterjvm.reviewboard.config.{Configs, HttpConfig, JWTConfig}
import com.misterjvm.reviewboard.http.HttpAPI
import com.misterjvm.reviewboard.http.controllers.{HealthController, ProgramController}
import com.misterjvm.reviewboard.repositories.*
import com.misterjvm.reviewboard.services.*
import sttp.tapir.*
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.{Server, ServerConfig}

import java.net.InetSocketAddress

object Application extends ZIOAppDefault {

  val configuredServer =
    Configs.makeLayer[HttpConfig]("misterjvm.http") >>>
      ZLayer(
        ZIO.service[HttpConfig].map(config => ServerConfig.default.copy(address = InetSocketAddress(config.port)))
      ) >>> Server.live

  val serverProgram = for {
    endpoints <- HttpAPI.endpointsZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default.appendInterceptor(
          CORSInterceptor.default
        )
      ).toHttp(endpoints)
    )
  } yield ()

  override def run =
    serverProgram.provide(
      configuredServer,
      // services
      ProgramServiceLive.layer,
      UserServiceLive.layer,
      JWTServiceLive.configuredLayer,
      EmailServiceLive.configuredLayer,
      InviteServiceLive.configuredLayer,
      PaymentServiceLive.configuredLayer,
      ReviewServiceLive.configuredLayer,
      OpenAIServiceLive.configuredLayer,
      // repos
      ProgramRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      TrainerRepositoryLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokensRepositoryLive.configuredLayer,
      InviteRepositoryLive.layer,
      // other requirements
      Repository.dataLayer
    )
}
