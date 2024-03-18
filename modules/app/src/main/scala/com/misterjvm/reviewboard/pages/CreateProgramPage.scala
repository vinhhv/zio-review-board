package com.misterjvm.reviewboard.pages

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.domain.data.{PaymentType, Trainer}
import com.misterjvm.reviewboard.http.requests.CreateProgramRequest
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.*
import org.scalajs.dom.html.Element
import zio.*

final case class CreateProgramState(
    name: String = "",
    url: String = "",
    trainerId: String = "",
    paymentType: String = "",
    image: Option[String] = None,
    tags: List[String] = List(),
    upstreamStatus: Option[Either[String, String]] = None,
    trainers: List[Trainer] = List(),
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
      PaymentType.fromOrdinal(paymentType.toInt),
      image,
      Option(tags).filter(_.nonEmpty)
    )
}

object CreateProgramPage extends FormPage[CreateProgramState]("Post New Program") {
  override def basicState: CreateProgramState = CreateProgramState()

  def getTrainers(): Unit =
    useBackend(_.user.getTrainersEndpoint(()))
      .map { trainers =>
        stateVar.update(
          _.copy(
            showStatus = false,
            trainers = trainers
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

  // TODO: Only show this page for admin (hard-coded for my email for now).
  // Otherwise, show contact form to email to be added.
  def apply() = {
    super.apply(getTrainers)
  }

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
          div(
            cls := "image-upload",
            input(
              `type` := "file",
              cls    := "form-control",
              idAttr := uid,
              accept := "image/*",
              onChange.mapToFiles --> fileUploader
            ),
            img(
              cls := "image-upload-thumbnail",
              src <-- stateVar.signal.map(_.image.getOrElse(Constants.programLogoPlaceholder))
            )
          )
        )
      )
    )

  private def computeDimensions(width: Int, height: Int): (Int, Int) =
    if (width >= height) {
      val ratio     = width * 1.0 / 256
      val newWidth  = width / ratio
      val newHeight = height / ratio
      (newWidth.toInt, newHeight.toInt)
    } else {
      val (newHeight, newWidth) = computeDimensions(height, width)
      (newWidth, newHeight)
    }

  val fileUploader = (files: List[File]) => {
    val maybeFile = files.headOption.filter(_.size > 0)
    maybeFile.foreach { file =>
      val reader = new FileReader
      reader.onload = _ => {
        // 256x256
        // draw the picture into a 256x256 canvas
        // make a fake img tag (not rendered) - img2
        val fakeImage = document.createElement("img").asInstanceOf[HTMLImageElement]
        fakeImage.addEventListener(
          "load",
          _ => {
            val canvas          = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
            val context         = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
            val (width, height) = computeDimensions(fakeImage.width, fakeImage.height)
            canvas.width = width
            canvas.height = height
            // render the original image into that canvas
            context.drawImage(fakeImage, 0, 0, width, height)
            // set the state to the text repr of img2
            stateVar.update(_.copy(image = Some(canvas.toDataURL(file.`type`))))
          }
        )
        fakeImage.src = reader.result.toString
      }
      reader.readAsDataURL(file)
    }
  }

  override def renderChildren(): List[ReactiveHtmlElement[Element]] =
    List(
      renderInput("Program Name", "name", "text", true, "PJF Performance", (s, v) => s.copy(name = v)),
      renderInput("Program URL", "url", "text", true, "https://pjfperformance.com", (s, v) => s.copy(url = v)),
      renderLogoUpload("Program logo", "logo"),
      div(child <-- stateVar.signal.map { s =>
        val options = s.trainers.map { trainer =>
          SelectOption(trainer.id.toString, trainer.name)
        }
        renderSelectInput("Program Trainer", "trainer", true, options, _.trainerId, (s, v) => s.copy(trainerId = v))
      }),
      renderSelectInput(
        "Payment Type",
        "payment_type",
        true,
        List(
          SelectOption("0", "Lifetime Access"),
          SelectOption("1", "Subscription"),
          SelectOption("2", "Lifetime Access or Subscription")
        ),
        _.paymentType,
        (s, v) => s.copy(paymentType = v)
      ),
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
    )
}
