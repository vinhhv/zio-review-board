package com.misterjvm.reviewboard.repositories

import com.misterjvm.reviewboard.config.{Configs, EmailVerificationTokensConfig}
import com.misterjvm.reviewboard.domain.data.EmailVerificationToken
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait EmailVerificationTokensRepository {
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

class EmailVerificationTokensRepositoryLive private (
    config: EmailVerificationTokensConfig,
    quill: Quill.Postgres[SnakeCase],
    userRepo: UserRepository
) extends EmailVerificationTokensRepository {
  import quill.*

  inline given schema: SchemaMeta[EmailVerificationToken] =
    schemaMeta[EmailVerificationToken]("email_verification_tokens")
  inline given insMeta: InsertMeta[EmailVerificationToken] = insertMeta[EmailVerificationToken]()
  inline given upMeta: UpdateMeta[EmailVerificationToken]  = updateMeta[EmailVerificationToken](_.email)

  private def randomUppercaseString(len: Int): Task[String] = // AB12CD34
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase)

  private def findToken(email: String): Task[Option[EmailVerificationToken]] =
    run(query[EmailVerificationToken].filter(_.email == lift(email))).map(_.headOption)

  private def replaceToken(email: String): Task[EmailVerificationToken] =
    for {
      token <- randomUppercaseString(8)
      emailToken <- run(
        query[EmailVerificationToken]
          .updateValue(
            lift(EmailVerificationToken(email, token, java.lang.System.currentTimeMillis() + config.duration))
          )
          .returning(r => r)
      )
    } yield emailToken

  private def generateToken(email: String): Task[EmailVerificationToken] =
    for {
      token <- randomUppercaseString(8)
      emailToken <- run(
        query[EmailVerificationToken]
          .insertValue(
            lift(EmailVerificationToken(email, token, java.lang.System.currentTimeMillis() + config.duration))
          )
          .returning(r => r)
      )
    } yield emailToken

  private def makeFreshToken(email: String): Task[EmailVerificationToken] =
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case None    => generateToken(email)
    }

  /**
   * 1. User tries to log in, but user is deactivated
   *    a. If token is still valid, say check your email
   *    b. If token is not valid, create a new token, then resend the email
   *    c. If token doesn't exist, something went out of sync, create a new one and resend the email
   */
  override def getToken(email: String): Task[Option[EmailVerificationToken]] =
    userRepo.getByEmail(email).flatMap {
      case None    => ZIO.none
      case Some(_) => makeFreshToken(email).map(Some(_))
    }

  override def checkToken(email: String, token: String): Task[Boolean] =
    for {
      now <- Clock.instant
      checkValid <-
        run(
          query[EmailVerificationToken].filter(r =>
            r.email == lift(email) && r.token == lift(token) && r.expiration > lift(now.toEpochMilli())
          )
        ).map(_.nonEmpty)
    } yield checkValid
}

object EmailVerificationTokensRepositoryLive {
  val layer = ZLayer {
    for {
      config   <- ZIO.service[EmailVerificationTokensConfig]
      quill    <- ZIO.service[Quill.Postgres[SnakeCase.type]]
      userRepo <- ZIO.service[UserRepository]
    } yield new EmailVerificationTokensRepositoryLive(config, quill, userRepo)
  }

  val configuredLayer =
    Configs.makeLayer[EmailVerificationTokensConfig]("misterjvm.recoverytokens") >>> layer
}
