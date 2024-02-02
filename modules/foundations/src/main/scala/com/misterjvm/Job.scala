package com.misterjvm

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

final case class Job(id: Long, title: String, url: String, company: String)

object Job {
  given codec: JsonCodec[Job] = DeriveJsonCodec.gen[Job] // macro-based JSON codec (generated)
}
