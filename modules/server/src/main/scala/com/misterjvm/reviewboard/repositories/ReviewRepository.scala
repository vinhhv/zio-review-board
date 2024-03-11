package com.misterjvm.reviewboard.repositories

import com.misterjvm.reviewboard.domain.data.{MetricScore, Review}
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait ReviewRepository {
  def create(review: Review): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByProgramId(programId: Long): Task[List[Review]]
  def getByProgramSlug(programSlug: String): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]
}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
  import quill.*

  inline given schema: SchemaMeta[Review]  = schemaMeta[Review]("reviews")
  inline given insMeta: InsertMeta[Review] = insertMeta[Review](_.id, _.created, _.updated)
  inline given upMeta: UpdateMeta[Review]  = updateMeta[Review](_.id, _.programId, _.userId, _.created)

  private given Encoder[MetricScore] = encoder(
    java.sql.Types.OTHER,
    (index, value, row) => row.setObject(index, value.toString, java.sql.Types.OTHER)
  )

  private given Decoder[MetricScore] = decoder(row => index => MetricScore.valueOf(row.getObject(index).toString))

  override def create(review: Review): Task[Review] =
    run(query[Review].insertValue(lift(review)).returning(r => r))

  override def getById(id: Long): Task[Option[Review]] =
    run(query[Review].filter(_.id == lift(id))).map(_.headOption)

  override def getByProgramId(programId: Long): Task[List[Review]] =
    run(query[Review].filter(_.programId == lift(programId)))

  override def getByProgramSlug(programSlug: String): Task[List[Review]] =
    run(query[Review].filter(_.programSlug == lift(programSlug)))

  override def getByUserId(userId: Long): Task[List[Review]] =
    run(query[Review].filter(_.userId == lift(userId)))

  override def update(id: Long, op: Review => Review): Task[Review] =
    for {
      current <- getById(id).someOrFail(new RuntimeException(s"Update review failed: missing ID $id"))
      updated <- run(query[Review].filter(_.id == lift(id)).updateValue(lift(op(current))).returning(r => r))
    } yield updated

  override def delete(id: Long): Task[Review] =
    run(query[Review].filter(_.id == lift(id)).delete.returning(r => r))

}

object ReviewRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase.type]].map(quill => ReviewRepositoryLive(quill))
  }
}
