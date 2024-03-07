package com.misterjvm.reviewboard.domain.data

import zio.json.JsonCodec

final case class ProgramFilter(
    trainers: List[String] = List(),
    paymentTypes: List[PaymentType] = List(),
    tags: List[String] = List()
) derives JsonCodec {
  val isEmpty = trainers.isEmpty && paymentTypes.isEmpty && tags.isEmpty
}

object ProgramFilter {
  val empty = ProgramFilter()
}
