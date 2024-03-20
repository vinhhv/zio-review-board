package com.misterjvm.reviewboard.config

final case class SummaryConfig(
    minReviews: Int,
    nSelected: Int,
    expiration: Int // in seconds
)
