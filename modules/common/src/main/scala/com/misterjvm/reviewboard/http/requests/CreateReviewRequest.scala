package com.misterjvm.reviewboard.http.requests

import com.misterjvm.reviewboard.domain.data.{MetricScore, Review}
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

  def fromReview(review: Review) = CreateReviewRequest(
    programId = review.programId,
    programSlug = review.programSlug,
    value = review.value,
    quality = review.quality,
    content = review.content,
    userExperience = review.userExperience,
    accessibility = review.accessibility,
    support = review.support,
    wouldRecommend = review.wouldRecommend,
    review = review.review
  )
}
