package com.misterjvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object NotFoundPage {
  def apply() =
    div("404 (Invalid page) - seems like you stepped out of bounds")
}
