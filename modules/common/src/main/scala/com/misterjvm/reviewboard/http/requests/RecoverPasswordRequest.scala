package com.misterjvm.reviewboard.http.requests

import zio.json.JsonCodec

final case class RecoverPasswordRequest(
    email: String,
    token: String,
    newPassword: String
) derives JsonCodec
