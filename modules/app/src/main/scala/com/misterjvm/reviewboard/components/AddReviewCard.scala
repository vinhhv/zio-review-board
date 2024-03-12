package com.misterjvm.reviewboard.components

import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.domain.data.{MetricScore, Review}
import com.misterjvm.reviewboard.http.requests.CreateReviewRequest
import com.misterjvm.reviewboard.pages.ProgramPage.maybeRenderUserAction
import com.raquo.laminar.api.L.{*, given}
import zio.*

class AddReviewCard(programId: Long, programSlug: String, onDisable: () => Unit, triggerBus: EventBus[Unit]) {

  final case class State(
      review: Review = Review.empty(programId, programSlug),
      showErrors: Boolean = false,
      upstreamError: Option[String] = None
  )

  val stateVar = Var(State())

  val submitter = Observer[State] { state =>
    if (state.upstreamError.nonEmpty) {
      stateVar.update(_.copy(showErrors = true))
    } else {
      useBackend(_.review.createEndpoint(CreateReviewRequest.fromReview(state.review)))
        .map { resp => onDisable() } // TODO notify the program page to refresh the review list
        .tapError(e =>
          ZIO.succeed {
            stateVar.update(
              _.copy(
                showErrors = true,
                upstreamError = Some(e.getMessage())
              )
            )
          }
        )
        .emitTo(triggerBus)
    }
  }

  def apply() =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "program-description add-review",
          div(
            // score dropdowns
            div(
              cls := "add-review-scores",
              renderDropdown("Value", (r, v) => r.copy(value = MetricScore.fromOrdinal(v))),
              renderDropdown("Quality", (r, v) => r.copy(quality = MetricScore.fromOrdinal(v))),
              renderDropdown("Content", (r, v) => r.copy(content = MetricScore.fromOrdinal(v))),
              renderDropdown("User Experience", (r, v) => r.copy(userExperience = MetricScore.fromOrdinal(v))),
              renderDropdown("Accessibility", (r, v) => r.copy(accessibility = MetricScore.fromOrdinal(v))),
              renderDropdown("Support", (r, v) => r.copy(support = MetricScore.fromOrdinal(v))),
              renderDropdown("Would Recommend", (r, v) => r.copy(wouldRecommend = MetricScore.fromOrdinal(v)))
            ),
            // text area for the text review
            div(
              cls := "add-review-text",
              label(forId := "add-review-text", "Your review - supports Markdown"),
              textArea(
                idAttr      := "add-review-text",
                cls         := "add-review-text-input",
                placeholder := "Write your review here",
                onInput.mapToValue --> stateVar.updater { (s: State, value: String) =>
                  s.copy(review = s.review.copy(review = value))
                }
              )
            ),
            button(
              `type` := "button",
              cls    := "btn btn-warning rock-action-btn",
              "Post review",
              onClick.preventDefault.mapTo(stateVar.now()) --> submitter
            ),
            a(
              cls  := "add-review-cancel",
              href := "#",
              "Cancel",
              onClick --> (_ => onDisable())
            ),
            children <-- stateVar.signal
              .map(s => s.upstreamError.filter(_ => s.showErrors))
              .map(maybeRenderError)
              .map(_.toList)
          )
        )
      )
    )

  private def renderDropdown(name: String, updateFn: (Review, Int) => Review) = {
    val selectorId = name.split(" ").map(_.toLowerCase).mkString("-")
    div(
      cls := "add-review-score",
      label(forId := selectorId, s"$name:"),
      select(
        idAttr := selectorId,
        (1 to 5).reverse.map { v =>
          option(
            v.toString,
            onInput.mapToValue --> stateVar.updater { (s: State, value: String) =>
              s.copy(review = updateFn(s.review, value.toInt))
            }
          )
          // TODO set state here
        }
      )
    )
  }

  private def maybeRenderError(maybeError: Option[String]) = maybeError.map { message =>
    div(
      cls := "page-status-errors",
      message
    )
  }

}
