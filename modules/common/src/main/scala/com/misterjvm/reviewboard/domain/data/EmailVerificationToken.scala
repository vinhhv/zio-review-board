package com.misterjvm.reviewboard.domain.data

final case class EmailVerificationToken(
    email: String,
    token: String,
    expiration: Long
)
