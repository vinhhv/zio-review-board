package com.misterjvm.reviewboard.repositories

import com.misterjvm.reviewboard.config.{Configs, RecoveryTokensConfig}
import com.misterjvm.reviewboard.domain.data.PasswordRecoveryToken
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait RecoveryTokensRepository {
  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]
}

/**
 * Verification tokens
 * 1. User signs up
 * 2. User is created in database, but deactivated
 * 2a. If user is already created, but token is still valid, show error stating account was created, check email for verification
 * 2b. If user is already created, but token is inactive, send another verification email and show status
 * 3. Email service sends email with verification link and token
 * 4. User clicks on link to verify
 * 5. Token is sent to the user service
 * 6. User is activated and asked to log in
 */

class RecoveryTokensRepositoryLive private (
    config: RecoveryTokensConfig,
    quill: Quill.Postgres[SnakeCase],
    userRepo: UserRepository
) extends RecoveryTokensRepository {
  import quill.*

  inline given schema: SchemaMeta[PasswordRecoveryToken]  = schemaMeta[PasswordRecoveryToken]("recovery_tokens")
  inline given insMeta: InsertMeta[PasswordRecoveryToken] = insertMeta[PasswordRecoveryToken]()
  inline given upMeta: UpdateMeta[PasswordRecoveryToken]  = updateMeta[PasswordRecoveryToken](_.email)

  private def randomUppercaseString(len: Int): Task[String] = // AB12CD34
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase)

  private def findToken(email: String): Task[Option[String]] =
    run(query[PasswordRecoveryToken].filter(_.email == lift(email))).map(_.headOption.map(_.token))

  private def replaceToken(email: String): Task[String] =
    for {
      token <- randomUppercaseString(8)
      _ <- run(
        query[PasswordRecoveryToken]
          .updateValue(
            lift(PasswordRecoveryToken(email, token, java.lang.System.currentTimeMillis() + config.duration))
          )
          .returning(r => r)
      )
    } yield token

  private def generateToken(email: String): Task[String] =
    for {
      token <- randomUppercaseString(8)
      _ <- run(
        query[PasswordRecoveryToken]
          .insertValue(
            lift(PasswordRecoveryToken(email, token, java.lang.System.currentTimeMillis() + config.duration))
          )
          .returning(r => r)
      )
    } yield token

  private def makeFreshToken(email: String): Task[String] =
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case None    => generateToken(email)
    }

  override def getToken(email: String): Task[Option[String]] =
    userRepo.getByEmail(email).flatMap {
      case None    => ZIO.none
      case Some(_) => makeFreshToken(email).map(Some(_))
    }

  override def checkToken(email: String, token: String): Task[Boolean] =
    for {
      now <- Clock.instant
      checkValid <-
        run(
          query[PasswordRecoveryToken].filter(r =>
            r.email == lift(email) && r.token == lift(token) && r.expiration > lift(now.toEpochMilli())
          )
        ).map(_.nonEmpty)
    } yield checkValid
}

object RecoveryTokensRepositoryLive {
  val layer = ZLayer {
    for {
      config   <- ZIO.service[RecoveryTokensConfig]
      quill    <- ZIO.service[Quill.Postgres[SnakeCase.type]]
      userRepo <- ZIO.service[UserRepository]
    } yield new RecoveryTokensRepositoryLive(config, quill, userRepo)
  }

  val configuredLayer =
    Configs.makeLayer[RecoveryTokensConfig]("misterjvm.recoverytokens") >>> layer
}
