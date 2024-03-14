package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.core.*
import com.misterjvm.reviewboard.http.requests.UpdatePasswordRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import zio.*

final case class ChangePasswordState(
    password: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  override val errorList: List[Option[String]] =
    List(
      Option.when(password.isEmpty)("Password can't be empty"),
      Option.when(newPassword.isEmpty)("Confirm password can't be empty"),
      Option.when(newPassword != confirmPassword)("Passwords must match")
    ) ++ upstreamStatus.map(_.left.toOption).toList

  override def maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)
}

object ChangePasswordPage extends FormPage[ChangePasswordState]("Change Password") {
  override def basicState = ChangePasswordState()

  def submitter(email: String) = Observer[ChangePasswordState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(
        _.user.updatePasswordEndpoint(
          UpdatePasswordRequest(email, state.password, state.newPassword)
        )
      ).map { userResponse =>
        stateVar.update(
          _.copy(
            showStatus = true,
            upstreamStatus = Some(Right("Password successfully changed."))
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
    Session.getUserState
      .map(_.email)
      .map(email =>
        List(
          renderInput(
            "Password",
            "password-input",
            "password",
            true,
            "Your password",
            (s, p) => s.copy(password = p, showStatus = false, upstreamStatus = None)
          ),
          renderInput(
            "New Password",
            "new-password-input",
            "password",
            true,
            "New password",
            (s, p) => s.copy(newPassword = p, showStatus = false, upstreamStatus = None)
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
            "Change Password",
            onClick.preventDefault.mapTo(stateVar.now()) --> submitter(email)
          )
        )
      )
      .getOrElse(
        List(
          div(
            cls := "centered-text",
            "Hold up! You haven't checked into the game yet. Log in to change your password."
          )
        )
      )
}
