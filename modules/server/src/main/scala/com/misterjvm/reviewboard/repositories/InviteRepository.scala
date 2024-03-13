package com.misterjvm.reviewboard.repositories

import com.misterjvm.reviewboard.domain.data.{InviteNamedRecord, InviteRecord, PaymentType, Program}
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait InviteRepository {
  def getByUsername(username: String): Task[List[InviteNamedRecord]]
  def getInvitePack(username: String, programId: Long): Task[Option[InviteRecord]]
  def addInvitePack(username: String, programId: Long, nInvites: Int): Task[Long]
  def activatePack(id: Long): Task[Boolean]
}

class InviteRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends InviteRepository with ProgramMeta(quill) {
  import quill.*

  inline given schema: SchemaMeta[InviteRecord]  = schemaMeta[InviteRecord]("invites")
  inline given insMeta: InsertMeta[InviteRecord] = insertMeta[InviteRecord](_.id)
  inline given upMeta: UpdateMeta[InviteRecord]  = updateMeta[InviteRecord](_.id)

  private given Encoder[PaymentType] = encoder(
    java.sql.Types.OTHER,
    (index, value, row) => row.setObject(index, value.toString, java.sql.Types.OTHER)
  )

  private given Decoder[PaymentType] = decoder(row => index => PaymentType.valueOf(row.getObject(index).toString))

  /**
   * SELECT programId, programName, nInvites
   * FROM invites, programs
   * WHERE invites.username = ?
   * AND invites.active
   * AND invites.nInvites > 0
   * AND invites.programId = programs.id
   */
  override def getByUsername(username: String): Task[List[InviteNamedRecord]] =
    run {
      for {
        record <- query[InviteRecord]
          .filter(_.username == lift(username))
          .filter(_.nInvites > 0)
          .filter(_.active)
        program <- query[Program] if program.id == record.id // join Program
      } yield InviteNamedRecord(program.id, program.name, record.nInvites)
    }

  override def getInvitePack(username: String, programId: Long): Task[Option[InviteRecord]] =
    run(
      query[InviteRecord]
        .filter(_.programId == lift(programId))
        .filter(_.username == lift(username))
        .filter(_.active)
    ).map(_.headOption)

  // WARNING: adding multiple active packs for the same program may cause unexpected behavior
  override def addInvitePack(username: String, programId: Long, nInvites: Int): Task[Long] =
    run(
      query[InviteRecord]
        .insertValue(lift(InviteRecord(-1, username, programId, nInvites, false)))
        .returning(_.id)
    )

  override def activatePack(id: Long): Task[Boolean] =
    for {
      current <- run(query[InviteRecord].filter(_.id == lift(id)))
        .map(_.headOption)
        .someOrFail(new RuntimeException(s"Unable to activate pack $id: no such pack"))
      result <- run(
        query[InviteRecord]
          .filter(_.id == lift(id))
          .updateValue(lift(current.copy(active = true)))
          .returning(_ => true)
      )
    } yield result

  // trigger addInvitePack (active false)
  // payment process
  // activate the pack that was just paid
}

object InviteRepositoryLive {
  val layer = ZLayer {
    for {
      quill <- ZIO.service[Quill.Postgres[SnakeCase.type]]
    } yield new InviteRepositoryLive(quill)
  }
}

object InviteRepositoryDemo extends ZIOAppDefault {
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = {
    val program = for {
      repo    <- ZIO.service[InviteRepository]
      records <- repo.getByUsername("vinh@misterjvm.com")
      _       <- Console.printLine(s"Records: ${records}")
    } yield ()

    program.provide(
      InviteRepositoryLive.layer,
      Repository.dataLayer
    )
  }
}
