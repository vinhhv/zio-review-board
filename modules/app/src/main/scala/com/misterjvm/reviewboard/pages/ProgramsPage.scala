package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.components.*
import com.misterjvm.reviewboard.domain.data.{PaymentType, Program}
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object ProgramsPage {

  val dummyProgram = Program(
    1L,
    "dummy-program",
    "Dummy Program",
    "http://dummyprograms.com",
    1L,
    PaymentType.LifetimeAccess,
    None,
    List("Ball Handling", "Weightlifting")
  )

  def apply() =
    sectionTag(
      cls := "section-1",
      div(
        cls := "container program-list-hero",
        h1(
          cls := "program-list-title",
          "Nothing But Net Programs"
        )
      ),
      div(
        cls := "container",
        div(
          cls := "row jvm-recent-programs-body",
          div(
            cls := "col-lg-4",
            div("TODO filter panel here")
          ),
          div(
            cls := "col-lg-8",
            renderProgram(dummyProgram),
            renderProgram(dummyProgram)
          )
        )
      )
    )

  private def renderImage(program: Program) =
    img(
      cls := "img-fluid",
      src := program.image.getOrElse(Constants.programLogoPlaceholder),
      alt := program.name
    )

  private def renderDetail(icon: String, value: String) =
    div(
      cls := "program-detail",
      i(cls := s"fa fa-$icon program-detail-icon"),
      p(
        cls := "program-detail-value",
        value
      )
    )

  private def renderOverview(program: Program) =
    div(
      cls := "program-summary",
      renderDetail(
        "person",
        program.trainerId.toString
      ), // TODO: Add trainer name to Program so we don't have to make another DB call
      renderDetail("tags", program.tags.mkString(", "))
    )

  private def renderAction(program: Program) =
    div(
      cls := "jvm-recent-programs-card-btn-apply",
      a(
        href   := program.url,
        target := "blank",
        button(
          `type` := "button",
          cls    := "btn btn-danger rock-action-btn",
          "Website"
        )
      )
    )

  def renderProgram(program: Program) =
    div(
      cls := "jvm-recent-programs-cards",
      div(
        cls := "jvm-recent-programs-card-img",
        renderImage(program)
      ),
      div(
        cls := "jvm-recent-programs-card-contents",
        h5(
          Anchors.renderNavLink(
            program.name,
            s"/program/${program.slug}",
            "program-title-link"
          )
        ),
        renderOverview(program)
      ),
      renderAction(program)
    )
}
