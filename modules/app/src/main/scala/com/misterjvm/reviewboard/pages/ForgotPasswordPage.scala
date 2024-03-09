package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.http.requests.ForgotPasswordRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import org.scalajs.dom.html.Element
import zio.*

final case class ForgotPasswordState(
    email: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  def errorList: List[Option[String]] =
    List(
      Option.when(!email.matches(Constants.emailRegex))("Email is invalid")
    ) ++ upstreamStatus.map(_.left.toOption).toList

  def maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)

}

object ForgotPasswordPage extends FormPage[ForgotPasswordState]("Forgot Password") {

  override val stateVar: Var[ForgotPasswordState] = Var(ForgotPasswordState())

  val submitter = Observer[ForgotPasswordState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(
        _.user.forgotPasswordEndpoint(
          ForgotPasswordRequest(state.email)
        )
      ).map { userResponse =>
        stateVar.update(
          _.copy(
            showStatus = true,
            upstreamStatus = Some(Right("Check your email!"))
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
      button(
        `type` := "button",
        "Recover Password",
        onClick.preventDefault.mapTo(stateVar.now()) --> submitter
      )
    )
}
