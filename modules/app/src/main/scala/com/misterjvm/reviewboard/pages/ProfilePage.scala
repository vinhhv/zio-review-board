package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.components.Anchors
import com.misterjvm.reviewboard.core.Session
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import zio.*

object ProfilePage {

  def apply() =
    div(
      cls := "row",
      div(
        cls := "col-md-5 p-0",
        div(
          cls := "logo",
          img(
            cls := "home-logo",
            src := Constants.logoImage,
            alt := "Nothing but Net"
          )
        )
      ),
      div(
        cls := "col-md-7",
        div(
          cls := "form-section",
          child <-- Session.userState.signal.map {
            case None    => renderInvalid()
            case Some(_) => renderContent()
          }
        )
      )
    )

  private def renderInvalid() = {
    div(
      cls := "top-section",
      h1(span("Tech!")),
      div("Hold up! You haven't checked into the game yet. Log in to see your profile.")
    )
  }

  private def renderContent() =
    div(
      cls := "top-section",
      h1(span("Profile")),
      // change password section
      div(
        cls := "profile-section",
        h3(span("Account Settings")),
        Anchors.renderNavLink("Change Password", "/changepassword")
      )
      // actions section - send invites for every program they have invites for
    )
}
