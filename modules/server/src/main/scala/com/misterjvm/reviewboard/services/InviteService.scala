package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.config.{Configs, InvitePackConfig}
import com.misterjvm.reviewboard.domain.data.InviteNamedRecord
import com.misterjvm.reviewboard.repositories.{InviteRepository, ProgramRepository}
import zio.*

trait InviteService {
  def getByUsername(username: String): Task[List[InviteNamedRecord]]
  def sendInvites(username: String, programId: Long, receivers: List[String]): Task[Int]
  def addInvitePack(username: String, programId: Long): Task[Long]
  def activatePack(id: Long): Task[Boolean]
}

class InviteServiceLive private (
    inviteRepo: InviteRepository,
    programRepo: ProgramRepository,
    emailService: EmailService,
    config: InvitePackConfig
) extends InviteService {
  override def getByUsername(username: String): Task[List[InviteNamedRecord]] =
    inviteRepo.getByUsername(username)

  // invariant: only one pack per user per program
  override def addInvitePack(username: String, programId: Long): Task[Long] =
    for {
      program <- programRepo
        .getById(programId)
        .someOrFail(new RuntimeException(s"Cannot invite to review: program $programId doesn't exist"))
      currentPack <- inviteRepo.getInvitePack(username, programId)
      newPackId <- currentPack match {
        case Some(_) => ZIO.fail(new RuntimeException("You already have an active pack for this program"))
        case None    => inviteRepo.addInvitePack(username, programId, config.nInvites)
      }
    } yield newPackId

  override def sendInvites(username: String, programId: Long, receivers: List[String]): Task[Int] =
    for {
      program <- programRepo
        .getById(programId)
        .someOrFail(
          new RuntimeException(s"Cannot send invites: program $programId does not exist")
        )
      nInvitesUsed <- inviteRepo.markInvitesUsed(username, programId, receivers.size)
      _ <- ZIO.collectAllPar(
        receivers
          .take(nInvitesUsed)
          .map(receiver => emailService.sendReviewInvite(username, receiver, program))
      )
    } yield nInvitesUsed

  override def activatePack(id: Long): Task[Boolean] =
    inviteRepo.activatePack(id)
}

object InviteServiceLive {
  val layer = ZLayer {
    for {
      inviteRepo   <- ZIO.service[InviteRepository]
      programRepo  <- ZIO.service[ProgramRepository]
      emailService <- ZIO.service[EmailService]
      config       <- ZIO.service[InvitePackConfig]
    } yield new InviteServiceLive(inviteRepo, programRepo, emailService, config)
  }

  val configuredLayer =
    Configs.makeLayer[InvitePackConfig]("misterjvm.invites") >>> layer
}
