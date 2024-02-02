package com.misterjvm.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

sealed trait ProgramType
case object LifetimeAccess extends ProgramType
case object Subscription   extends ProgramType

object ProgramType {
  given codec: JsonCodec[ProgramType] = DeriveJsonCodec.gen[ProgramType]
}
