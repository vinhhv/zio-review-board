package com.misterjvm.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

final case class Review(
    id: Long,
    programId: Long,
    userId: Long,
    // scores
    value: MetricScore,
    quality: MetricScore,
    content: MetricScore,
    userExperience: MetricScore,
    accessibility: MetricScore,
    support: MetricScore,
    wouldRecommend: MetricScore,
    review: String,
    created: Instant,
    updated: Instant
)

object Review {
  given codec: JsonCodec[Review] = DeriveJsonCodec.gen[Review]
}
