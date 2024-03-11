package com.misterjvm.reviewboard.http.requests

import com.misterjvm.reviewboard.domain.data.MetricScore
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class CreateReviewRequest(
    programId: Long,
    programSlug: String,
    value: MetricScore,
    quality: MetricScore,
    content: MetricScore,
    userExperience: MetricScore,
    accessibility: MetricScore,
    support: MetricScore,
    wouldRecommend: MetricScore,
    review: String
)

object CreateReviewRequest {
  given codec: JsonCodec[CreateReviewRequest] = DeriveJsonCodec.gen[CreateReviewRequest]
}
