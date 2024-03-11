package com.misterjvm.reviewboard.components

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.domain.data.Program
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object ProgramComponents {
  def renderProgramLogo(program: Program) =
    img(
      cls := "img-fluid",
      src := program.image.getOrElse(Constants.programLogoPlaceholder),
      alt := program.name
    )

  def renderDetail(icon: String, value: String, maybeLink: Option[String] = None) =
    div(
      cls := "program-detail",
      i(cls := s"fa fa-$icon program-detail-icon"), {
        maybeLink match
          case None =>
            p(
              cls := "program-detail-value",
              value
            )
          case Some(link) =>
            a(
              href   := link,
              target := "blank",
              p(
                cls := "program-detail-value",
                value
              )
            )
      }
    )

  def renderOverview(program: Program) =
    div(
      cls := "program-summary",
      renderDetail(
        "person",
        program.trainerName,
        // TODO: replace with slug
        Some(s"/trainer/${program.trainerId}")
      ),
      renderDetail("credit-card", program.paymentType.toString),
      renderDetail("tags", program.tags.mkString(", "))
    )
}
