package com.misterjvm.reviewboard.http.requests

import com.misterjvm.reviewboard.domain.data.*
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class CreateProgramRequest(
    name: String,
    url: String,
    trainer: String,
    paymentType: PaymentType,
    image: Option[String] = None,
    tags: Option[List[String]] = None
) {
  def toProgram(id: Long) =
    Program(id, Program.makeSlug(name), name, url, trainer, paymentType, image, tags.getOrElse(List()))
}

object CreateProgramRequest {
  given codec: JsonCodec[CreateProgramRequest] = DeriveJsonCodec.gen[CreateProgramRequest]
}
