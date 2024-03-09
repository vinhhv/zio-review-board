package com.misterjvm.reviewboard.components

import com.misterjvm.reviewboard.pages.*
import com.raquo.laminar.api.L.{*, given}
import frontroute.PageStatusCode.NotFound
import frontroute.*
import org.scalajs.dom

object Router {
  def apply() =
    mainTag( // <main>
      routes(
        div(
          cls := "container-fluid",
          // potential children
          (pathEnd | path("programs")) { // localhost:1234/ or localhost:8080/ or localhost:1234/programs
            ProgramsPage()
          },
          path("login") {
            LoginPage()
          },
          path("signup") {
            SignUpPage()
          },
          path("profile") {
            ProfilePage()
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
          noneMatched {
            NotFoundPage()
          }
        )
      )
    )
}
