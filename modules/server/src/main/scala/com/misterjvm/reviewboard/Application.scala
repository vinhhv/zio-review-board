package com.misterjvm.reviewboard

import com.misterjvm.reviewboard.http.controllers.HealthController
import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val serverProgram = for {
    controller <- HealthController.makeZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(controller.health)
    )
    _ <- Console.printLine("Mister JVM!")
  } yield ()

  override def run =
    serverProgram.provide(
      Server.default
    )
}
