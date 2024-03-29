package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.components.*
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.domain.data.Program
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import zio.*

object ProgramsPage {

  // components
  val filterPanel = new FilterPanel

  val firstBatch = EventBus[List[Program]]()

  val programEvents: EventStream[List[Program]] =
    firstBatch.events.mergeWith {
      filterPanel.triggerFilters.flatMap { newFilter =>
        useBackend(_.program.searchEndpoint(newFilter)).toEventStream
      }
    }

  def apply() =
    sectionTag(
      onMountCallback(_ => useBackend(_.program.getAllEndpoint(())).emitTo(firstBatch)),
      cls := "section-1",
      div(
        cls := "container program-list-hero",
        img(
          cls := "program-list-hero-background",
          src := Constants.basketballHeroImage,
          alt := "Basketball Hero"
        ),
        h1(
          cls := "program-list-title",
          "Swish Programs: Basketball Training Program Reviews"
        )
      ),
      div(
        cls := "container",
        div(
          cls := "row jvm-recent-programs-body",
          div(
            cls := "col-lg-4",
            filterPanel()
          ),
          div(
            cls := "col-lg-8",
            children <-- programEvents.map(_.map(renderProgram))
          )
        )
      )
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
        ProgramComponents.renderProgramLogo(program)
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
        ProgramComponents.renderOverview(program)
      ),
      renderAction(program)
    )
}
