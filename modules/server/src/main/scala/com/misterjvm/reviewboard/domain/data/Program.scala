package com.misterjvm.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class Program(
    id: Long,
    slug: String, // pjf-performance-ball-handling-code
    name: String, // "PJF Performance Ball Handling Code" -> companies.misterjvm.com/company/my-company-inc
    url: String,
    trainer: String, // Paul J. Fabritz
    programType: ProgramType,
    image: Option[String] = None,
    tags: List[String] = List()
)

object Program {
  given codec: JsonCodec[Program] = DeriveJsonCodec.gen[Program]
}
