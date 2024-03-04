package com.misterjvm.reviewboard.repositories

import zio.*

trait RecoveryTokensRepository {
  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]
}

class RecoveryTokensRepositoryLive private extends RecoveryTokensRepository {
  override def getToken(email: String): Task[Option[String]]           = ???
  override def checkToken(email: String, token: String): Task[Boolean] = ???
}

object RecoveryTokensRepositoryLive {
  val layer = ZLayer.succeed(new RecoveryTokensRepositoryLive)
}
