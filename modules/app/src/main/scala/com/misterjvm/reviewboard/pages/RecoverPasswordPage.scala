package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.components.Anchors
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.http.requests.RecoverPasswordRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import zio.*

final case class RecoverPasswordState(
    email: String = "",
    token: String = "",
    password: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  def errorList: List[Option[String]] =
    List(
      Option.when(!email.matches(Constants.emailRegex))("Email is invalid"),
      Option.when(token.isEmpty)("Token can't be empty"),
      Option.when(password.isEmpty)("Password can't be empty"),
      Option.when(password != confirmPassword)("Passwords must match")
    ) ++ upstreamStatus.map(_.left.toOption).toList

  def maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)

}

object RecoverPasswordPage extends FormPage[RecoverPasswordState]("Reset Password") {

  override def basicState = RecoverPasswordState()

  val submitter = Observer[RecoverPasswordState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(
        _.user.recoverPasswordEndpoint(
          RecoverPasswordRequest(state.email, state.token, state.password)
        )
      ).map { _ =>
        stateVar.update(
          _.copy(
            showStatus = true,
            upstreamStatus = Some(Right("Success! Please log in now."))
          )
        )
      }.tapError { e =>
        ZIO.succeed {
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus = Some(Left(e.getMessage()))
            )
          )
        }
      }.runJS
    }
  }

  override def renderChildren(): List[ReactiveHtmlElement[HTMLElement]] =
    List(
      renderInput(
        "Email",
        "email-input",
        "email",
        true,
        "Your email",
        (s, e) => s.copy(email = e, showStatus = false, upstreamStatus = None)
      ),
      renderInput(
        "Token (from email)",
        "token-input",
        "token",
        true,
        "Your token",
        (s, t) => s.copy(token = t, showStatus = false, upstreamStatus = None)
      ),
      renderInput(
        "New Password",
        "new-password-input",
        "password",
        true,
        "New password",
        (s, p) => s.copy(password = p, showStatus = false, upstreamStatus = None)
      ),
      renderInput(
        "Confirm Password",
        "confirm-password-input",
        "password",
        true,
        "Confirm password",
        (s, p) => s.copy(confirmPassword = p, showStatus = false, upstreamStatus = None)
      ),
      button(
        `type` := "button",
        "Reset Password",
        onClick.preventDefault.mapTo(stateVar.now()) --> submitter
      ),
      Anchors.renderNavLink(
        "Need a password recovery token?",
        "/forgot",
        "auth-link"
      )
    )
}
