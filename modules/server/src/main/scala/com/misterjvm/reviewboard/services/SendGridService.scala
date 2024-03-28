package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.config.{Configs, SendGridServiceConfig}
import com.misterjvm.reviewboard.domain.data.{PaymentType, Program}
import com.sendgrid.*
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.{Content, Email}
import zio.*

import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}

trait SendGridService(baseUrl: String, fromAddress: String) {
  def sendEmail(from: String, to: String, subject: String, content: String): Task[Unit]

  def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] = {
    val subject = "Swish Programs: Password Recovery"
    val content =
      s"""
        <div style="
          border: 1px solid black;
          padding: 20px;
          font-family: sans-serif;
          line-height: 2;
          font-size: 20px;
        ">
        <div>
          <h1>Swish Programs 🏀: Password Recovery</h1>
          <p>Your password recovery token is: <strong>$token</strong></p>
          <p>
            Go <a href="$baseUrl/recover">here</a>
          </p>
          <p>Please reset your password using the token above.</p>
        </div>
      """

    sendEmail(fromAddress, to, subject, content)
  }

  def sendReviewInvite(username: String, to: String, program: Program): Task[Unit] = {
    val subject = s"Swish Programs: Invitation to review ${program.name} from $username"
    val content =
      s"""
        <div style="
          border: 1px solid black;
          padding: 20px;
          font-family: sans-serif;
          line-height: 2;
          font-size: 20px;
        ">
        <div>
          <h1>You're invited by $username to review ${program.name}</h1>
          <p>
            Go to
            <a href="$baseUrl/program/${program.slug}">this link</a>
            to add your thoughts on the app.
            <br/>
            Should just take a minute.
          </p>
        </div>
      """
    sendEmail(fromAddress, to, subject, content)
  }
}

class SendGridServiceLive private (config: SendGridServiceConfig)
    extends SendGridService(config.baseUrl, config.fromAddress) {
  override def sendEmail(from: String, to: String, subject: String, content: String): Task[Unit] = {
    for {
      mail     <- createMail(from, to, subject, content)
      request  <- createRequest(mail)
      sg       <- ZIO.succeed(new SendGrid(config.apiKey))
      response <- ZIO.attempt(sg.api(request))
      _ <- response.getStatusCode() match {
        case status if status >= 200 && status <= 299 => ZIO.unit
        case error => Console.printLineError(s"Error sending email ($error): ${response.getBody()}")
      }
    } yield ()
  }

  private def createMail(from: String, to: String, subject: String, content: String): Task[Mail] = {
    val fromEmail    = new Email(from)
    val toEmail      = new Email(to)
    val emailContent = new Content("text/html", content)
    ZIO.succeed(new Mail(fromEmail, subject, toEmail, emailContent))
  }

  private def createRequest(mail: Mail): Task[Request] = {
    ZIO.attempt {
      val request = new Request()
      request.setMethod(Method.POST)
      request.setEndpoint("mail/send")
      request.setBody(mail.build())
      request
    }
  }
}

object SendGridServiceLive {
  val layer = ZLayer {
    ZIO.service[SendGridServiceConfig].map(config => new SendGridServiceLive(config))
  }

  val configuredLayer =
    Configs.makeLayer[SendGridServiceConfig]("misterjvm.sendgrid") >>> layer
}

object SendGridServiceDemo extends ZIOAppDefault {
  val program = Program(
    1L,
    "pjf-performance-unranked-academy",
    "PJF Performance",
    "https://swishprograms.com/program/pjf-performance-unranked-academy",
    1L,
    "Paul J. Fabritz",
    PaymentType.Subscription
  )

  val runProgram = for {
    emailService <- ZIO.service[SendGridService]
    _ <- emailService.sendEmail(
      "info@swishprograms.com",
      "[REPLACE_ME]",
      "Hi from MisterJVM",
      "This email service works!"
    )
    _ <- Clock.sleep(Duration.fromSeconds(3))
    _ <- emailService.sendPasswordRecoveryEmail("[REPLACE_ME]", "ABCD1234")
    _ <- Clock.sleep(Duration.fromSeconds(3))
    _ <- emailService.sendReviewInvite("vince@swishprograms.com", "[REPLACE_ME]", program)
    _ <- Console.printLine("Email done.")
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    runProgram.provide(SendGridServiceLive.configuredLayer)
}
