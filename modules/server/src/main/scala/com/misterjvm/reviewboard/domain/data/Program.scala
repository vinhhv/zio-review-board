package com.misterjvm.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class Program(
    id: Long,
    slug: String, // pjf-performance-ball-handling-code
    name: String, // "PJF Performance Ball Handling Code" -> hoops.com/programs/pjf-performance-ball-handling-code
    url: String,
    trainer: String, // Paul J. Fabritz
    programType: ProgramType,
    image: Option[String] = None,
    tags: List[String] = List()
)

object Program {
  given codec: JsonCodec[Program] = DeriveJsonCodec.gen[Program]

  def makeSlug(name: String): String =
    name
      .replaceAll(" +", " ")
      .split(" ")
      .map((_.toLowerCase()))
      .mkString("-")
}
