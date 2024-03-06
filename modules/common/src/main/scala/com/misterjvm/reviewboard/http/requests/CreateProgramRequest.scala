package com.misterjvm.reviewboard.http.requests

import zio.json.{DeriveJsonCodec, JsonCodec}
import com.misterjvm.reviewboard.domain.data.{PaymentType, Program}

final case class CreateProgramRequest(
    name: String,
    url: String,
    trainerId: Long,
    paymentType: PaymentType,
    image: Option[String] = None,
    tags: Option[List[String]] = None
) {
  def toProgram(id: Long) =
    Program(id, Program.makeSlug(name), name, url, trainerId, paymentType, image, tags.getOrElse(List()))
}

object CreateProgramRequest {
  given codec: JsonCodec[CreateProgramRequest] = DeriveJsonCodec.gen[CreateProgramRequest]
}
