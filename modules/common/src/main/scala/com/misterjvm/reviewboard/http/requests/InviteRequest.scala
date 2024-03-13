package com.misterjvm.reviewboard.http.requests

import zio.json.JsonCodec

final case class InviteRequest(
    programId: Long,
    emails: List[String]
) derives JsonCodec
