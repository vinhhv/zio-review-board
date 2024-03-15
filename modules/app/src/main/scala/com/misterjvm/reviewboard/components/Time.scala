package com.misterjvm.reviewboard.components

import scala.scalajs.*
import scala.scalajs.js.*
import scala.scalajs.js.annotation.*

// In order to use JS libraries, we must create the necessary classes & functions that match the API

@js.native
@JSGlobal
class Moment extends js.Object {
  def format(): String  = js.native
  def fromNow(): String = js.native
}

// m = moment.unix(125123) => Moment object
// m.format("...") => "..."
@js.native
@JSImport("moment", JSImport.Default)
object MomentLib extends js.Object {
  def unix(millis: Long): Moment = js.native
}

// API that will be used in the application
object Time {
  def unix2hr(millis: Long) =
    MomentLib.unix(millis / 1000).fromNow()

  def past(millis: Long) =
    new Date().getTime.toLong - millis
}
