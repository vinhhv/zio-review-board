package com.misterjvm.reviewboard.config

final case class EmailServiceConfig(
    host: String,
    port: Int,
    user: String,
    pass: String
)
