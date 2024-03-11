package com.misterjvm.reviewboard.domain.data

final case class Trainer(
    id: Long,
    name: String, // Paul Fabritz
    description: String,
    url: String,
    image: Option[String] = None
)
