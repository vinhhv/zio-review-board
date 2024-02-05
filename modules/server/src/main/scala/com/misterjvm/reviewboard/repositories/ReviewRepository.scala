package com.misterjvm.reviewboard.repositories

import com.misterjvm.reviewboard.domain.data.Review
import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

trait ReviewRepository {
  def create(review: Review): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByProgramId(id: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]
}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {

  override def getByProgramId(id: Long): Task[List[Review]] = ???

  override def getById(id: Long): Task[Option[Review]] = ???

  override def getByUserId(userId: Long): Task[List[Review]] = ???

  override def delete(id: Long): Task[Review] = ???

  override def create(review: Review): Task[Review] = ???

  override def update(id: Long, op: Review => Review): Task[Review] = ???

}

object ReviewRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase.type]].map(quill => ReviewRepositoryLive(quill))
  }
}
