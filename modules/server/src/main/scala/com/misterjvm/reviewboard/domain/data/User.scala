package com.misterjvm.reviewboard.domain.data

final case class User(
    id: Long,
    email: String,
    hashedPassword: String
) {
  def toUserId: UserID = UserID(id, email)
}

final case class UserID(
    id: Long,
    email: String
)
