package com.misterjvm.reviewboard.http.requests

import zio.json.JsonCodec

final case class InvitePackRequest(programId: Long) derives JsonCodec
