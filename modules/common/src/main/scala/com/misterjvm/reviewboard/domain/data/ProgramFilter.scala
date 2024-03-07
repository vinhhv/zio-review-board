package com.misterjvm.reviewboard.domain.data

import zio.json.JsonCodec

final case class ProgramFilter(
    trainers: List[String] = List(),
    paymentTypes: List[PaymentType] = List(),
    tags: List[String] = List()
) derives JsonCodec

object ProgramFilter {
  val empty = ProgramFilter()
}
