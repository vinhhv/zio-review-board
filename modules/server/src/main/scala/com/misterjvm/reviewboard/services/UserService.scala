package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.domain.data.{User, UserToken}
import com.misterjvm.reviewboard.repositories.UserRepository
import zio.*

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import com.misterjvm.reviewboard.repositories.UserRepositoryLive

trait UserService {
  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def deleteUser(email: String, password: String): Task[User]
  // JWT
  def generateToken(email: String, password: String): Task[Option[UserToken]]
}

class UserServiceLive private (jwtService: JWTService, userRepo: UserRepository) extends UserService {

  override def registerUser(email: String, password: String): Task[User] =
    userRepo.create(
      User(
        id = -1L,
        email = email,
        hashedPassword = UserServiceLive.Hasher.generateHash(password)
      )
    )

  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(UserServiceLive.nonexistentUserError(email))
      result <- ZIO.attempt(UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword))
    } yield result

  override def generateToken(email: String, password: String): Task[Option[UserToken]] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(UserServiceLive.nonexistentUserError(email))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      maybeToken <- jwtService.createToken(existingUser).when(verified)
    } yield maybeToken

  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(UserServiceLive.nonexistentUserError(email))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(oldPassword, existingUser.hashedPassword)
      )
      updatedUser <- userRepo
        .update(
          existingUser.id,
          user => user.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword))
        )
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not update password for $email"))
    } yield updatedUser

  override def deleteUser(email: String, password: String): Task[User] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(UserServiceLive.nonexistentUserError(email))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      updatedUser <- userRepo
        .delete(existingUser.id)
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not delete user for $email"))
    } yield updatedUser
}

object UserServiceLive {
  val layer = ZLayer {
    for {
      jwtService <- ZIO.service[JWTService]
      userRepo   <- ZIO.service[UserRepository]
    } yield new UserServiceLive(jwtService, userRepo)
  }

  def nonexistentUserError(email: String): RuntimeException = new RuntimeException(
    s"Cannot verify user $email: nonexistent"
  )

  object Hasher {
    private val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS: Int   = 1000
    private val SALT_BYTE_SIZE: Int      = 24
    private val HASH_BYTE_SIZE: Int      = 24
    private val skf: SecretKeyFactory    = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    private def pbkdf2(message: Array[Char], salt: Array[Byte], iterations: Int, nBytes: Int): Array[Byte] = {
      val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, nBytes * 8)
      skf.generateSecret(keySpec).getEncoded()
    }

    private def toHex(array: Array[Byte]): String = // hex-encoded bytes
      array.map(b => "%02X".format(b)).mkString

    private def fromHex(hex: String): Array[Byte] = {
      hex.sliding(2, 2).toArray.map { hexValue =>
        Integer.parseInt(hexValue, 16).toByte
      }
    }

    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean = {
      val range = 0 until math.min(a.length, b.length)
      val diff = range.foldLeft(a.length ^ b.length) { case (acc, i) =>
        acc | (a(i) ^ b(i))
      }
      diff == 0
    }

    // string + salt + nIterations PBKDF2
    // "1000:AAAAAAAAA:BBBBBBBBB"
    def generateHash(string: String): String = {
      val rng: SecureRandom = new SecureRandom()
      val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt) // creates 24 random bytes

      val hashBytes = pbkdf2(string.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      s"$PBKDF2_ITERATIONS:${toHex(salt)}:${toHex(hashBytes)}"
    }
    def validateHash(string: String, hash: String): Boolean = {
      val hashSegments = hash.split(":")
      val nIterations  = hashSegments(0).toInt
      val salt         = fromHex(hashSegments(1))
      val validHash    = fromHex(hashSegments(2))
      val testHash     = pbkdf2(string.toCharArray(), salt, nIterations, HASH_BYTE_SIZE)
      compareBytes(testHash, validHash)
    }
  }
}

object UserServiceDemo {
  def main(args: Array[String]) = {
    val hash = UserServiceLive.Hasher.generateHash("misterjvm")
    println(hash)
    println(UserServiceLive.Hasher.validateHash("misterjvm", hash))
  }
}
