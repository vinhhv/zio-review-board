package com.misterjvm.reviewboard.http.requests

import com.misterjvm.reviewboard.domain.data.*
import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

final case class CreateProgramRequest(
    name: String,
    url: String,
    trainer: String,
    programType: ProgramType,
    image: Option[String] = None,
    tags: Option[List[String]] = None
) {
  def toProgram(id: Long) =
    Program(id, Program.makeSlug(name), name, url, trainer, programType, image, tags.getOrElse(List()))
}

object CreateProgramRequest {
  given codec: JsonCodec[CreateProgramRequest] = DeriveJsonCodec.gen[CreateProgramRequest]
}
