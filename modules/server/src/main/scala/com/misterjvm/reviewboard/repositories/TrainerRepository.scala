package com.misterjvm.reviewboard.repositories

import com.misterjvm.reviewboard.domain.data.Trainer
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait TrainerRepository {
  def getAll: Task[List[Trainer]]
  def getById(id: Long): Task[Option[Trainer]]
}

class TrainerRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends TrainerRepository {
  import quill.*

  inline given schema: SchemaMeta[Trainer]  = schemaMeta[Trainer]("trainers")
  inline given insMeta: InsertMeta[Trainer] = insertMeta[Trainer](_.id)
  inline given upMeta: UpdateMeta[Trainer]  = updateMeta[Trainer](_.id)

  override def getAll: Task[List[Trainer]] =
    run {
      query[Trainer]
    }

  override def getById(id: Long): Task[Option[Trainer]] =
    run {
      query[Trainer]
        .filter(_.id == lift(id))
    }.map(_.headOption)
}

object TrainerRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase.type]].map(quill => TrainerRepositoryLive(quill))
  }
}
