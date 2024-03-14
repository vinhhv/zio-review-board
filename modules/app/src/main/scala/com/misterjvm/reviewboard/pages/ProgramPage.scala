package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.components.{AddReviewCard, Markdown, ProgramComponents, Router, Time}
import com.misterjvm.reviewboard.core.Session
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.http.requests.InvitePackRequest
import com.raquo.laminar.DomApi
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import zio.*

import java.time.Instant

object ProgramPage {

  enum Status {
    case LOADING
    case NOT_FOUND
    case OK(program: Program)
  }

  // reactive variables

  val addReviewCardActive = Var[Boolean](false)
  val fetchProgramBus     = EventBus[Option[Program]]()
  val triggerRefreshBus   = EventBus[Unit]()

  def refreshReviewList(programSlug: String) =
    useBackend(_.review.getByProgramSlugEndpoint(programSlug)).toEventStream
      .mergeWith(
        triggerRefreshBus.events
          .flatMap(_ => useBackend(_.review.getByProgramSlugEndpoint(programSlug)).toEventStream)
      )

  def reviewsSignal(programSlug: String): Signal[List[Review]] = fetchProgramBus.events
    .flatMap {
      case None          => EventStream.empty
      case Some(program) => refreshReviewList(programSlug)
    }
    .scanLeft(List[Review]())((_, list) => list)

  val status = fetchProgramBus.events.scanLeft(Status.LOADING) { (_, maybeProgram) =>
    maybeProgram match {
      case None          => Status.NOT_FOUND
      case Some(program) => Status.OK(program)
    }
  }

  val inviteErrorBus = EventBus[String]()

  def startPaymentFlow(programId: Long) =
    useBackend(_.invite.addPackPromotedEndpoint(InvitePackRequest(programId)))
      .tapError(e => ZIO.succeed(inviteErrorBus.emit(e.getMessage())))
      .emitTo(Router.externalUrlBus)

  // renderer

  def apply(slug: String) =
    div(
      cls := "container-fluid the-rock",
      onMountCallback(_ => useBackend(_.program.getByIdEndpoint(slug)).emitTo(fetchProgramBus)),
      children <-- status.map {
        case Status.LOADING     => List(div("Loading..."))
        case Status.NOT_FOUND   => List(div("Program not found."))
        case Status.OK(program) => render(program, reviewsSignal(slug))
      }
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
        child <-- Session.userState.signal.map(maybeUser => maybeRenderUserAction(maybeUser, reviewsSignal))
      )
    ),
    div(
      cls := "container-fluid",
      renderProgramSummary, // TODO: fill summary later
      children <-- addReviewCardActive.signal
        .map(active =>
          Option.when(active)(
            AddReviewCard(
              program.id,
              program.slug,
              onDisable = () => addReviewCardActive.set(false),
              triggerRefreshBus
            )()
          )
        )
        .map(_.toList),
      children <-- reviewsSignal.map(_.map(renderReview)),
      child.maybe <-- Session.userState.signal.map(_.map(_ => renderInviteAction(program)))
    )
  )

  def renderInviteAction(program: Program) =
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
            button(
              `type` := "button",
              cls    := "rock-action-btn",
              "Invite people",
              disabled <-- inviteErrorBus.events.mapTo(true).startWith(false),
              onClick.mapToUnit --> (_ => startPaymentFlow(program.id))
            ),
            div(
              child.text <-- inviteErrorBus.events
            )
          )
        )
      )
    )

  def maybeRenderUserAction(maybeUser: Option[UserToken], reviewsSignal: Signal[List[Review]]) =
    maybeUser match {
      case None =>
        div(
          cls := "jvm-programs-details-card-apply-now-btn",
          "You must be logged in to post a review"
        )
      case Some(user) =>
        div(
          cls := "jvm-programs-details-card-apply-now-btn",
          child <-- reviewsSignal
            .map(_.find(_.userId == user.id))
            .map {
              case None =>
                button(
                  `type` := "button",
                  cls    := "btn btn-warning",
                  "Add a review",
                  disabled <-- addReviewCardActive.signal,
                  onClick.mapTo(true) --> addReviewCardActive.writer
                )
              case Some(_) =>
                div("You already posted a review")
            }
        )
    }

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
        // add a css class to "your review"
        cls.toggle("review-highlighted") <-- Session.userState.signal.map(maybeUser =>
          maybeUser.map(_.id) == Option(review).map(_.userId)
        ),
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
          injectMarkdown(review),
          div(cls := "review-posted", s"Posted ${Time.unix2hr(review.created.toEpochMilli())}"),
          child.maybe <-- Session.userState.signal
            .map(
              _.filter(_.id == review.userId)
            )
            .map(_.map(_ => div(cls := "review-posted", "Your review")))
        )
      )
    )

  def injectMarkdown(review: Review) =
    div(
      cls := "review-content",
      DomApi
        .unsafeParseHtmlStringIntoNodeArray(Markdown.toHtml(review.review))
        .map {
          case t: dom.Text         => span(t.data)
          case e: dom.html.Element => foreignHtmlElement(e)
          case _                   => emptyNode
        }
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
