package com.misterjvm.reviewboard.domain.data

final case class Trainer(
    id: Long,
    slug: String, // paul-fabritz
    name: String, // Paul Fabritz
    description: String,
    image: Option[String] = None
)
