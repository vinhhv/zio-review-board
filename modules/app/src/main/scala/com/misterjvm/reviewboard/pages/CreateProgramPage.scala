package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.domain.data.PaymentType
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.html.Element
import com.misterjvm.reviewboard.http.requests.CreateProgramRequest
import zio.*
import org.scalajs.dom.File
import org.scalajs.dom.FileReader

final case class CreateProgramState(
    name: String = "",
    url: String = "",
    trainerId: String = "",
    paymentType: String = "",
    image: Option[String] = None,
    tags: List[String] = List(),
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  def errorList: List[Option[String]] =
    List(
      Option.when(name.isEmpty)("The name can't be empty"),
      Option.when(url.isEmpty)("The URL can't be empty"),
      Option.when(!url.matches(Constants.urlRegex))("The URL is invalid"),
      Option.when(trainerId.isEmpty)("A trainer must be selected"),
      Option.when(paymentType.isEmpty)("A payment type must be selected")
    ) ++ upstreamStatus.map(_.left.toOption).toList

  def maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)

  def toRequest: CreateProgramRequest =
    CreateProgramRequest(
      name,
      url,
      trainerId.toLong,
      "",
      PaymentType.fromOrdinal(paymentType.toInt),
      image,
      Option(tags).filter(_.nonEmpty)
    )
}

object CreateProgramPage extends FormPage[CreateProgramState]("Post New Program") {
  override def basicState: CreateProgramState = CreateProgramState()

  val submitter = Observer[CreateProgramState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.program.createEndpoint(state.toRequest))
        .map { program =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus = Some(Right("Program posted!"))
            )
          )
        }
        .tapError(e =>
          ZIO.succeed {
            stateVar.update(
              _.copy(
                showStatus = true,
                upstreamStatus = Some(Left(e.getMessage()))
              )
            )
          }
        )
        .runJS
    }
  }

  private def renderLogoUpload(name: String, uid: String, isRequired: Boolean = false) =
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
            `type` := "file",
            cls    := "form-control",
            idAttr := uid,
            accept := "image/*",
            onChange.mapToFiles --> fileUploader
          )
        )
      )
    )

  val fileUploader = (files: List[File]) => {
    val maybeFile = files.headOption.filter(_.size > 0)
    maybeFile.foreach { file =>
      val reader = new FileReader
      reader.onload = _ => {
        stateVar.update(_.copy(image = Some(reader.result.toString)))
      }
      reader.readAsDataURL(file)
    }
  }

  override def renderChildren(): List[ReactiveHtmlElement[Element]] =
    List(
      renderInput("Program Name", "name", "text", true, "PJF Performance", (s, v) => s.copy(name = v)),
      renderInput("Program URL", "url", "text", true, "https://pjfperformance.com", (s, v) => s.copy(url = v)),
      renderLogoUpload("Program logo", "logo"),
      renderInput("Program Trainer", "trainer", "number", true, "1", (s, v) => s.copy(trainerId = v)),
      renderInput("Payment Type", "payment_type", "number", true, "0", (s, v) => s.copy(paymentType = v)),
      renderInput(
        "Tags - separate by ','",
        "tags",
        "text",
        false,
        "Ball Handling, Vert",
        (s, v) => s.copy(tags = v.split(",").map(_.trim).toList)
      ),
      button(
        `type` := "button",
        "Post Program",
        onClick.preventDefault.mapTo(stateVar.now()) --> submitter
      )
      // name
      // url
      // logo - file upload
      // trainer
      // payment type
      // tags
    )
}
