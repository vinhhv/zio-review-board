package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.common.*
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.core.*
import com.misterjvm.reviewboard.http.requests.LoginRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontroute.BrowserNavigation
import org.scalajs.dom
import zio.*

trait FormState {
  def errorList: List[Option[String]]
  def showStatus: Boolean
  def maybeSuccess: Option[String]

  def maybeError = errorList.find(_.isDefined).flatten
  def hasErrors  = errorList.exists(_.isDefined)

  def maybeStatus: Option[Either[String, String]] =
    maybeError.map(Left(_)).orElse(maybeSuccess.map(Right(_))).filter(_ => showStatus)
}

abstract class FormPage[S <: FormState](title: String) {
  val stateVar: Var[S]

  def renderChildren(): List[ReactiveHtmlElement[dom.html.Element]]

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
        // right
        div(
          cls := "form-section",
          div(cls := "top-section", h1(span(title))),
          children <-- stateVar.signal
            .map(_.maybeStatus)
            .map(renderStatus)
            .map(_.toList),
          form(
            nameAttr := "signin",
            cls      := "form",
            idAttr   := "form"
          ),
          renderChildren()
        )
      )
    )

  def renderStatus(status: Option[Either[String, String]]) = status.map {
    case Left(error) =>
      div(
        cls := "page-status-errors",
        error
      )
    case Right(message) =>
      div(
        cls := "page-status-success",
        message
      )
  }

  def renderInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      placeHolder: String,
      updateFn: (S, String) => S
  ) =
    div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := uid,
            cls   := "form-label",
            if (isRequired) span("*") else span(),
            name
          ),
          input(
            `type`      := kind,
            cls         := "form-control",
            idAttr      := uid,
            placeholder := placeHolder,
            onInput.mapToValue --> stateVar.updater(updateFn)
          )
        )
      )
    )
}
