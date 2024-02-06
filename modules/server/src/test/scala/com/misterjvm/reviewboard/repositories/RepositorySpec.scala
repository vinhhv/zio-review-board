package com.misterjvm.reviewboard.repositories

import com.misterjvm.reviewboard.domain.data.{MetricScore, PaymentType, Program, Review}
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import zio.*
import zio.test.*

import java.time.Instant
import javax.sql.DataSource

trait RepositorySpec {

  val initScript: String

  // spawn a Postgres instance on Docker for the test
  private def createContainer() = {
    val container: PostgreSQLContainer[Nothing] =
      PostgreSQLContainer("postgres").withInitScript(initScript)
    container.start
    container
  }

  // create a Datasource to connect to the Postgres
  private def createDataSource(container: PostgreSQLContainer[Nothing]): DataSource = {
    val dataSource = new PGSimpleDataSource()
    dataSource.setUrl(container.getJdbcUrl())
    dataSource.setUser(container.getUsername())
    dataSource.setPassword(container.getPassword())
    dataSource
  }

  // use the Datasource (as a ZLayer) to build the Quill instance (as a ZLayer)
  val dataSourceLayer = ZLayer {
    for {
      container <- ZIO.acquireRelease(ZIO.attempt(createContainer()))(container =>
        ZIO.attempt(container.stop()).ignoreLogged
      )
      dataSource <- ZIO.attempt(createDataSource(container))
    } yield dataSource
  }
}
