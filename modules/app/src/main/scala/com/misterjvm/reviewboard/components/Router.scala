package com.misterjvm.reviewboard.components

import com.misterjvm.reviewboard.pages.*
import com.raquo.laminar.api.L.{*, given}
import frontroute.PageStatusCode.NotFound
import frontroute.*
import org.scalajs.dom

object Router {
  val externalUrlBus = EventBus[String]()

  def apply() =
    mainTag(
      onMountCallback(ctx => externalUrlBus.events.foreach(url => dom.window.location.href = url)(ctx.owner)),
      routes(
        div(
          cls := "container-fluid",
          (pathEnd | path("programs")) {
            ProgramsPage()
          },
          path("login") {
            LoginPage()
          },
          path("signup") {
            SignUpPage()
          },
          path("changepassword") {
            ChangePasswordPage()
          },
          path("forgot") {
            ForgotPasswordPage()
          },
          path("recover") {
            RecoverPasswordPage()
          },
          path("logout") {
            LogoutPage()
          },
          path("profile") {
            ProfilePage()
          },
          path("post") {
            CreateProgramPage()
          },
          path("program" / segment) { programSlug =>
            ProgramPage(programSlug)
          },
          noneMatched {
            NotFoundPage()
          }
        )
      )
    )
}
