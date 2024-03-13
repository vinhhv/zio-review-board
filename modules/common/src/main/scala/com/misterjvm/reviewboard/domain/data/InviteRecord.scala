package com.misterjvm.reviewboard.domain.data

final case class InviteRecord(
    id: Long,
    username: String,
    programId: Long,
    nInvites: Int,
    active: Boolean = false
)
