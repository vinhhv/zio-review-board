package com.misterjvm.reviewboard.repositories

import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import zio.ZLayer

import javax.sql.DataSource

object Repository {

  def dataSourceLayer: ZLayer[Any, Throwable, DataSource] =
    Quill.DataSource.fromPrefix("misterjvm.db")

  def quillLayer: ZLayer[DataSource, Nothing, Postgres[SnakeCase.type]] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  val dataLayer: ZLayer[Any, Throwable, Postgres[SnakeCase.type]] =
    dataSourceLayer >>> quillLayer
}
