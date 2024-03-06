package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.components.*
import com.misterjvm.reviewboard.domain.data.{PaymentType, Program}
import com.misterjvm.reviewboard.http.endpoints.ProgramEndpoints
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

object ProgramsPage {

  val dummyProgram = Program(
    1L,
    "dummy-program",
    "Dummy Program",
    "http://dummyprograms.com",
    1L,
    "Dummy Dum",
    PaymentType.LifetimeAccess,
    None,
    List("Ball Handling", "Weightlifting")
  )

  val programsBus = EventBus[List[Program]]()

  def performBackendCall(): Unit = {
    // fetch API
    // AJAX
    // ZIO endpoints
    val programEndpoints                   = new ProgramEndpoints {}
    val theEndpoint                        = programEndpoints.getAllEndpoint
    val backend                            = FetchZioBackend()
    val interpreter: SttpClientInterpreter = SttpClientInterpreter()
    val request = interpreter
      .toRequestThrowDecodeFailures(theEndpoint, Some(uri"http://localhost:8080"))
      .apply(())

    val programsZIO = backend.send(request).map(_.body).absolve
    // run the ZIO effect

    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.fork(
        programsZIO.tap(list => ZIO.attempt(programsBus.emit(list)))
      )
    }
  }

  def apply() =
    sectionTag(
      onMountCallback(_ => performBackendCall()),
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
            children <-- programsBus.events.map(_.map(renderProgram))
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

  private def renderDetail(icon: String, value: String, maybeLink: Option[String] = None) =
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

  private def renderOverview(program: Program) =
    div(
      cls := "program-summary",
      renderDetail(
        "person",
        program.trainerName,
        Some(s"/trainer/${program.trainerId}")
      ),
      renderDetail("credit-card", program.paymentType.toString),
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
