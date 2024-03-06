package com.misterjvm.reviewboard.common

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Constants {
  @js.native
  @JSImport("/static/img/basketball.jpg", JSImport.Default)
  val logoImage: String = js.native

  @js.native
  @JSImport("/static/img/generic_program.png", JSImport.Default)
  val programLogoPlaceholder: String = js.native
}
