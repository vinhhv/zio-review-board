package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.domain.data.{User, UserID, UserToken}
import com.misterjvm.reviewboard.repositories.{RecoveryTokensRepository, UserRepository}
import zio.*
import zio.test.*
import com.misterjvm.reviewboard.repositories.TrainerRepositoryLive
import com.misterjvm.reviewboard.repositories.TrainerRepository
import com.misterjvm.reviewboard.domain.data.Trainer

object UserServiceSpec extends ZIOSpecDefault {

  val vinh = User(
    1L,
    "vinh@misterjvm.com",
    "1000:DB3721B69E32D219F13297E77F48F41CFB5358D854890583:F5FD7EEA32AAABE03D00B799B6B01EA01EA2483551B8791D"
  )

  val stubRepoLayer = ZLayer.succeed {
    new UserRepository {
      val db = collection.mutable.Map[Long, User](1L -> vinh)
      def create(user: User): Task[User] =
        ZIO.succeed {
          db += (user.id -> user)
          user
        }

      def getById(id: Long): Task[Option[User]] =
        ZIO.succeed(db.get(id))

      def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))

      def update(id: Long, op: User => User): Task[User] =
        ZIO.attempt {
          val newUser = op(db(id))
          db += (newUser.id -> newUser)
          newUser
        }

      def delete(id: Long): Task[User] =
        ZIO.attempt {
          val user = db(id)
          db -= id
          user
        }
    }
  }

  val stubTokenRepoLayer = ZLayer.succeed {
    new RecoveryTokensRepository {
      val db = collection.mutable.Map[String, String]()

      override def checkToken(email: String, token: String): Task[Boolean] =
        ZIO.succeed(db.get(email).filter(_ == token).nonEmpty)

      override def getToken(email: String): Task[Option[String]] =
        ZIO.attempt {
          val token = util.Random.alphanumeric.take(8).mkString.toUpperCase
          db += (email -> token)
          Some(token)
        }
    }
  }

  val stubTrainerLayer = ZLayer.succeed {
    new TrainerRepository {
      override def getAll: Task[List[Trainer]] = ZIO.succeed(List())

      override def getById(id: Long): Task[Option[Trainer]] = ZIO.none
    }
  }

  val stubEmailsLayer = ZLayer.succeed {
    new EmailService("http://someurl.com") {
      override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit
    }
  }

  val stubJwtLayer = ZLayer.succeed {
    new JWTService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.id, user.email, "BIG ACCESS", Long.MaxValue))

      override def verifyToken(token: String): Task[UserID] =
        ZIO.succeed(UserID(vinh.id, vinh.email))
    }
  }

  val password = "misterjvm"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserServiceSpec")(
      test("create and validate a user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.registerUser(vinh.email, password)
          valid   <- service.verifyPassword(vinh.email, password)
        } yield assertTrue(valid && user.email == vinh.email)
      },
      test("validate correct credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(vinh.email, password)
        } yield assertTrue(valid)
      },
      test("invalidate incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(vinh.email, "wrong password")
        } yield assertTrue(!valid)
      },
      test("invalidate non-existent user") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("someone@gmail.com", password)
        } yield assertTrue(!valid)
      },
      test("update password") {
        for {
          service  <- ZIO.service[UserService]
          newUser  <- service.updatePassword(vinh.email, password, "scalarulez")
          oldValid <- service.verifyPassword(vinh.email, password)
          newValid <- service.verifyPassword(vinh.email, "scalarulez")
        } yield assertTrue(newValid && !oldValid)
      },
      test("delete non-existent user should fail") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.deleteUser("someone@gmail.com", password).flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete user with incorrect credentials should fail") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.deleteUser(vinh.email, "wrong password").flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.deleteUser(vinh.email, password)
        } yield assertTrue(user.email == vinh.email)
      }
    ).provide(
      UserServiceLive.layer,
      stubTrainerLayer,
      stubJwtLayer,
      stubRepoLayer,
      stubEmailsLayer,
      stubTokenRepoLayer
    )
}
