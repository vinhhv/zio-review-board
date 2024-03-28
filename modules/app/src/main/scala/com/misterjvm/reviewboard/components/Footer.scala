package com.misterjvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import scala.scalajs.js.Date

object Footer {
  def apply() = div(
    cls := "main-footer",
    div(
      "Written in Scala & ZIO by ",
      a(href := "https://github.com/vinhhv", "MisterJVM")
    ),
    div("For any questions, please email us at info@swishprograms.com"),
    div(s"©️ ${new Date().getFullYear()} all rights reserved.")
  )
}
