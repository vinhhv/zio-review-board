package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.domain.data.InviteNamedRecord
import com.misterjvm.reviewboard.repositories.{InviteRepository, ProgramRepository}
import zio.*

trait InviteService {
  def getByUsername(username: String): Task[List[InviteNamedRecord]]
  def sendInvites(username: String, programId: Long, receivers: List[String]): Task[Int]
  def addInvitePack(username: String, programId: Long): Task[Long]
}

class InviteServiceLive private (inviteRepo: InviteRepository, programRepo: ProgramRepository) extends InviteService {
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
        case None    => inviteRepo.addInvitePack(username, programId, 200) // TODO: configure nInvites
      }
      // TODO: remove when introducing payment process
      _ <- inviteRepo.activatePack(newPackId)
    } yield newPackId

  override def sendInvites(username: String, programId: Long, receivers: List[String]): Task[Int] = ???
}

object InviteServiceLive {
  val layer = ZLayer {
    for {
      inviteRepo  <- ZIO.service[InviteRepository]
      programRepo <- ZIO.service[ProgramRepository]
    } yield new InviteServiceLive(inviteRepo, programRepo)
  }
}
