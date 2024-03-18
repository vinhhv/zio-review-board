package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.config.{Configs, FlywayConfig}
import org.flywaydb.core.Flyway
import zio.*

trait FlywayService {
  def runClean: Task[Unit]
  def runBaseline: Task[Unit]
  def runMigrations: Task[Unit] // look at changes we specified
  def runRepairs: Task[Unit]
}

class FlywayServiceLive(flyway: Flyway) extends FlywayService {
  override def runClean: Task[Unit] =
    ZIO.attemptBlocking(flyway.clean())

  override def runBaseline: Task[Unit] =
    ZIO.attemptBlocking(flyway.baseline())

  override def runMigrations: Task[Unit] =
    ZIO.attemptBlocking(flyway.migrate())

  override def runRepairs: Task[Unit] =
    ZIO.attemptBlocking(flyway.repair())
}

object FlywayServiceLive {
  val layer = ZLayer {
    for {
      config <- ZIO.service[FlywayConfig]
      flyway <- ZIO.attempt(
        Flyway
          .configure()
          .dataSource(config.url, config.user, config.password)
          .load()
      )
    } yield FlywayServiceLive(flyway)
  }

  val configuredLayer =
    Configs.makeLayer[FlywayConfig]("misterjvm.db.dataSource") >>> layer
}
