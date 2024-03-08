package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.core.Session
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Element

final case class LogoutPageState() extends FormState {
  override def errorList: List[Option[String]] = List()
  override def maybeSuccess: Option[String]    = None
  override def showStatus: Boolean             = false
}

object LogoutPage extends FormPage[LogoutPageState]("Log Out") {
  override val stateVar: Var[LogoutPageState] = Var(LogoutPageState())

  override def renderChildren(): List[ReactiveHtmlElement[Element]] = List(
    div(
      onMountCallback(_ => Session.clearUserState()),
      cls := "logout-status",
      "You have been successfully logged out."
    )
  )
}
