package com.misterjvm.reviewboard.domain.data

import zio.json.JsonCodec

final case class InviteNamedRecord(
    programId: Long,
    programName: String,
    nInvites: Int
) derives JsonCodec
