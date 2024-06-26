package com.misterjvm.reviewboard.repositories

import com.misterjvm.reviewboard.domain.data.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait ProgramRepository {
  def create(program: Program): Task[Program]
  def getById(id: Long): Task[Option[Program]]
  def getBySlug(slug: String): Task[Option[Program]]
  def get: Task[List[Program]]
  def update(id: Long, op: Program => Program): Task[Program]
  def delete(id: Long): Task[Program]
  def uniqueAttributes: Task[ProgramFilter]
  def search(filter: ProgramFilter): Task[List[Program]]
}

class ProgramRepositoryLive private (quill: Quill.Postgres[SnakeCase])
    extends ProgramRepository
    with ProgramMeta(quill) {
  import quill.*

  private given Encoder[PaymentType] = encoder(
    java.sql.Types.OTHER,
    (index, value, row) => row.setObject(index, value.toString, java.sql.Types.OTHER)
  )

  private given Decoder[PaymentType] = decoder(row => index => PaymentType.valueOf(row.getObject(index).toString))

  override def create(program: Program): Task[Program] =
    run {
      query[Program]
        .insertValue(lift(program))
        .returning(program => program)
    }

  override def getById(id: Long): Task[Option[Program]] =
    run {
      query[Program].filter(_.id == lift(id))
    }.map(_.headOption)

  override def getBySlug(slug: String): Task[Option[Program]] =
    run {
      query[Program].filter(_.slug == lift(slug))
    }.map(_.headOption)

  override def get: Task[List[Program]] =
    run(query[Program])

  override def update(id: Long, op: Program => Program): Task[Program] =
    for {
      current <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing ID $id"))
      updated <- run {
        query[Program]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(program => program)
      }
    } yield updated

  override def delete(id: Long): Task[Program] =
    run {
      query[Program]
        .filter(_.id == lift(id))
        .delete
        .returning(program => program)
    }

  override def uniqueAttributes: Task[ProgramFilter] =
    for {
      trainers     <- run(query[Program].map(_.trainerName).distinct)
      paymentTypes <- run(query[Program].map(_.paymentType).distinct)
      tags         <- run(query[Program].map(_.tags)).map(_.flatten.toSet.toList)
    } yield ProgramFilter(trainers, paymentTypes, tags)

  override def search(filter: ProgramFilter): Task[List[Program]] =
    if (filter.isEmpty) get
    else
      run {
        query[Program]
          .filter { program =>
            liftQuery(filter.trainers.toSet).contains(program.trainerName) ||
            liftQuery(filter.paymentTypes.toSet).contains(program.paymentType) ||
            sql"${program.tags} && ${lift(filter.tags)}".asCondition
          }
      }
}

object ProgramRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(quill => ProgramRepositoryLive(quill))
  }
}

object ProgramRepositoryDemo extends ZIOAppDefault {

  val program = for {
    repo <- ZIO.service[ProgramRepository]
    // _ <- repo.create(
    //   Program(-1L, "pjf-performance", "PJF Performance", "pjf.com", "Paul J. Fabritz", PaymentType.LifetimeAccess)
    // )
    programs <- repo.get
    _        <- Console.printLine(programs)
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    program.provide(
      ProgramRepositoryLive.layer,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("misterjvm.db")
    )
}
