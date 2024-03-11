package com.misterjvm.reviewboard.domain.data

final case class Trainer(
    id: Long,
    // TODO: Add slug
    name: String, // Paul Fabritz
    description: String,
    url: String,
    image: Option[String] = None
)
