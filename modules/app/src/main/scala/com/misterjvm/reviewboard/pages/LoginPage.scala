package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.common.*
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.core.*
import com.misterjvm.reviewboard.http.requests.LoginRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontroute.BrowserNavigation
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import zio.*

final case class LoginFormState(
    email: String = "",
    password: String = "",
    upstreamError: Option[String] = None,
    override val showStatus: Boolean = false
) extends FormState {
  private val userEmailError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("User email is invalid")

  private val passwordError: Option[String] =
    Option.when(password.isEmpty)("Password can't be empty")

  override val errorList: List[Option[String]] = List(userEmailError, passwordError, upstreamError)
  override val maybeSuccess: Option[String]    = None
}

object LoginPage extends FormPage[LoginFormState]("Log In") {
  override val stateVar = Var(LoginFormState())

  val submitter = Observer[LoginFormState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.user.loginEndpoint(LoginRequest(state.email, state.password)))
        .map { userToken =>
          Session.setUserState(userToken)
          stateVar.set(LoginFormState())
          BrowserNavigation.replaceState("/")
        }
        .tapError(e =>
          ZIO.succeed {
            stateVar.update(_.copy(showStatus = true, upstreamError = Some(e.getMessage())))
          }
        )
        .runJS
    }
  }

  override def renderChildren(): List[ReactiveHtmlElement[HTMLElement]] =
    List(
      renderInput(
        "Email",
        "email-input",
        "text",
        true,
        "Your email",
        (s, e) => s.copy(email = e, showStatus = false, upstreamError = None)
      ),
      renderInput(
        "Password",
        "password-input",
        "password",
        true,
        "Your password",
        (s, p) => s.copy(password = p, showStatus = false, upstreamError = None)
      ),
      button(
        `type` := "button",
        "Log In",
        onClick.preventDefault.mapTo(stateVar.now()) --> submitter
      )
    )
}
