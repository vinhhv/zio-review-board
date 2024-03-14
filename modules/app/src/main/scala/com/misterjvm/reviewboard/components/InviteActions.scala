package com.misterjvm.reviewboard.components

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.domain.data.InviteNamedRecord
import com.misterjvm.reviewboard.http.requests.InviteRequest
import com.misterjvm.reviewboard.pages.ProgramPage.refreshReviewList
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import zio.*

object InviteActions {

  val inviteListBus = EventBus[List[InviteNamedRecord]]()

  def refreshInviteList() =
    useBackend(_.invite.getByUserIdEndpoint(()))

  def apply() =
    div(
      onMountCallback(_ => refreshInviteList().emitTo(inviteListBus)),
      cls := "profile-section",
      h3(span("Invites")),
      children <-- inviteListBus.events.map(_.sortBy(_.programName).map(renderInviteSection))
    )

  def renderInviteSection(record: InviteNamedRecord) = {
    val emailListVar  = Var[Array[String]](Array())
    val maybeErrorVar = Var[Option[String]](None)

    val canSubmitVar = Var[Boolean](false)

    val inviteSubmitter = Observer[Unit] { _ =>
      val emailList = emailListVar.now().toList
      if (emailList.exists(!_.matches(Constants.emailRegex))) {
        maybeErrorVar.set(Some("At least one email is invalid"))
        canSubmitVar.set(false)
      } else {
        canSubmitVar.set(false)
        val refreshProgram = for {
          _           <- useBackend(_.invite.invitedEndpoint(InviteRequest(record.programId, emailList)))
          invitesLeft <- refreshInviteList()
        } yield invitesLeft

        maybeErrorVar.set(None)
        refreshProgram.emitTo(inviteListBus)
      }
    }

    div(
      cls := "invite-section",
      h5(span(record.programName)),
      p(s"${record.nInvites} invites left"),
      textArea(
        cls         := "invites-area",
        placeholder := "Enter emails, one per line",
        onInput.mapToValue.map(_.split("\n").map(_.trim).filter(_.nonEmpty)) --> emailListVar.writer,
        onInput.mapToValue --> { _ =>
          if (emailListVar.now().nonEmpty) {
            canSubmitVar.set(true)
          } else if (emailListVar.now().isEmpty) {
            canSubmitVar.set(false)
          }
        }
      ),
      button(
        `type` := "button",
        cls    := "btn btn-primary",
        "Invite",
        disabled <-- canSubmitVar.signal.map(!_),
        onClick.mapToUnit --> inviteSubmitter
      ),
      child.maybe <-- maybeErrorVar.signal.map(maybeRenderError)
    )
  }

  private def maybeRenderError(maybeError: Option[String]) = maybeError.map { message =>
    div(
      cls := "page-status-errors",
      message
    )
  }
}
