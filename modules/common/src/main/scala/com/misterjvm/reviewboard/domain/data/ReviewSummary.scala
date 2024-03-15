package com.misterjvm.reviewboard.domain.data

import zio.json.JsonCodec

import java.time.Instant

final case class ReviewSummary(
    programId: Long,
    contents: String,
    created: Instant
) derives JsonCodec
