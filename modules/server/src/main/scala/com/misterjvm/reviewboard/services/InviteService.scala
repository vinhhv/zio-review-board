package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.domain.data.InviteNamedRecord
import com.misterjvm.reviewboard.repositories.{InviteRepository, ProgramRepository}
import zio.*
import com.misterjvm.reviewboard.config.InvitePackConfig
import com.misterjvm.reviewboard.config.Configs

trait InviteService {
  def getByUsername(username: String): Task[List[InviteNamedRecord]]
  def sendInvites(username: String, programId: Long, receivers: List[String]): Task[Int]
  def addInvitePack(username: String, programId: Long): Task[Long]
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
      // TODO: remove when introducing payment process
      _ <- inviteRepo.activatePack(newPackId)
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
