package com.misterjvm.reviewboard.config

final case class EmailVerificationTokensConfig(
    duration: Long // token duration in milliseconds
)
