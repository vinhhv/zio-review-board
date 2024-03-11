package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.components.ProgramComponents
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.domain.data.*
import com.raquo.laminar.api.L.{*, given}
import zio.*

import java.time.Instant

object ProgramPage {

  // dummy data
  val dummyReviews = List(
    Review(
      1L,
      1L,
      "pjf-performance-unranked-academy",
      1L,
      MetricScore.Amazing,
      MetricScore.Amazing,
      MetricScore.Amazing,
      MetricScore.Amazing,
      MetricScore.Amazing,
      MetricScore.Amazing,
      MetricScore.Amazing,
      "Amazing program",
      Instant.now(),
      Instant.now()
    ),
    Review(
      1L,
      1L,
      "pjf-performance-unranked-academy",
      1L,
      MetricScore.Fair,
      MetricScore.Fair,
      MetricScore.Fair,
      MetricScore.Fair,
      MetricScore.Fair,
      MetricScore.Fair,
      MetricScore.Fair,
      "Iz Okay",
      Instant.now(),
      Instant.now()
    ),
    Review(
      1L,
      1L,
      "pjf-performance-unranked-academy",
      1L,
      MetricScore.Great,
      MetricScore.Great,
      MetricScore.Great,
      MetricScore.Great,
      MetricScore.Great,
      MetricScore.Great,
      MetricScore.Great,
      "Pretty good, but missing some features",
      Instant.now(),
      Instant.now()
    )
  )

  enum Status {
    case LOADING
    case NOT_FOUND
    case OK(program: Program)
  }

  // reactive variables

  val fetchProgramBus = EventBus[Option[Program]]()
  def reviewsSignal(programSlug: String): Signal[List[Review]] = fetchProgramBus.events
    .flatMap {
      case None => EventStream.empty
      case Some(program) =>
        val reviewsBus = EventBus[List[Review]]()
        useBackend(_.review.getByProgramSlugEndpoint(programSlug)).emitTo(reviewsBus)
        reviewsBus.events
    }
    .scanLeft(List[Review]())((_, list) => list)

  val status = fetchProgramBus.events.scanLeft(Status.LOADING) { (_, maybeProgram) =>
    maybeProgram match {
      case None          => Status.NOT_FOUND
      case Some(program) => Status.OK(program)
    }
  }

  // renderer

  def apply(slug: String) =
    div(
      cls := "container-fluid the-rock",
      onMountCallback(_ => useBackend(_.program.getByIdEndpoint(slug)).emitTo(fetchProgramBus)),
      children <-- status.map {
        case Status.LOADING     => List(div("Loading..."))
        case Status.NOT_FOUND   => List(div("Program not found."))
        case Status.OK(program) => render(program, reviewsSignal(slug))
      },
      child <-- reviewsSignal(slug).map(_.toString)
    )

  def render(program: Program, reviewsSignal: Signal[List[Review]]) = List(
    div(
      cls := "row jvm-programs-details-top-card",
      div(
        cls := "col-md-12 p-0",
        div(
          cls := "jvm-programs-details-card-profile-img",
          ProgramComponents.renderProgramLogo(program)
        ),
        div(
          cls := "jvm-programs-details-card-profile-title",
          h1(program.name),
          div(
            cls := "jvm-programs-details-card-profile-program-details-program-and-location",
            ProgramComponents.renderOverview(program)
          )
        ),
        div(
          cls := "jvm-programs-details-card-apply-now-btn",
          button(
            `type` := "button",
            cls    := "btn btn-warning",
            "Add a review"
          )
        )
      )
    ),
    div(
      cls := "container-fluid",
      renderProgramSummary, // TODO: fill summary later
      children <-- reviewsSignal.map(_.map(renderReview)),
      div(
        cls := "container",
        div(
          cls := "rok-last",
          div(
            cls := "row invite-row",
            div(
              cls := "col-md-6 col-sm-6 col-6",
              span(
                cls := "rock-apply",
                p("Do you represent this program?"),
                p("Invite people to leave reviews.")
              )
            ),
            div(
              cls := "col-md-6 col-sm-6 col-6",
              a(
                href   := program.url,
                target := "blank",
                button(`type` := "button", cls := "rock-action-btn", "Invite people")
                // TODO: invite new people to review the program
              )
            )
          )
        )
      )
    )
  )

  def renderProgramSummary =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "program-description",
          "TODO program summary"
        )
      )
    )

  def renderReview(review: Review) =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        // TODO add a highlight if this is "your" review
        div(
          cls := "program-description",
          div(
            cls := "review-summary",
            renderReviewDetail("Value", review.value),
            renderReviewDetail("Quality", review.quality),
            renderReviewDetail("Content", review.content),
            renderReviewDetail("User Experience", review.userExperience),
            renderReviewDetail("Accessibility", review.accessibility),
            renderReviewDetail("Support", review.support),
            renderReviewDetail("Would Recommend", review.wouldRecommend)
          ),
          // TODO parse this Markdown
          div(
            cls := "review-content",
            review.review
          ),
          div(cls := "review-posted", "Posted (TODO) a million years ago")
        )
      )
    )

  def renderReviewDetail(detail: String, metricScore: MetricScore) =
    div(
      cls := "review-detail",
      span(cls := "review-detail-name", s"$detail: "),
      (1 to metricScore.score).toList.map(_ =>
        svg.svg(
          svg.cls     := "review-rating",
          svg.viewBox := "0 0 32 32",
          svg.path(
            svg.d := "m15.1 1.58-4.13 8.88-9.86 1.27a1 1 0 0 0-.54 1.74l7.3 6.57-1.97 9.85a1 1 0 0 0 1.48 1.06l8.62-5 8.63 5a1 1 0 0 0 1.48-1.06l-1.97-9.85 7.3-6.57a1 1 0 0 0-.55-1.73l-9.86-1.28-4.12-8.88a1 1 0 0 0-1.82 0z"
          )
        )
      )
    )
}
