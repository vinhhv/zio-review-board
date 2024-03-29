package com.misterjvm.reviewboard.common

import org.scalajs.dom.window

import scala.scalajs.js.annotation.*
import scala.scalajs.{js, LinkingInfo}

object Constants {
  @js.native
  @JSImport("/static/img/basketball.jpg", JSImport.Default)
  val logoImage: String = js.native

  @js.native
  @JSImport("/static/img/generic_program.png", JSImport.Default)
  val programLogoPlaceholder: String = js.native

  @js.native
  @JSImport("/static/img/basketball_hero.jpeg", JSImport.Default)
  val basketballHeroImage: String = js.native

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""

  val urlRegex = """^(https?):\/\/(([^:/?#]+)(?::(\d+))?)(\/[^?#]*)?(\?[^#]*)?(#.*)?"""

  val backendBaseUrl =
    if (LinkingInfo.developmentMode) "http://localhost:4041"
    else window.location.origin
}
