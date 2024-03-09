package com.misterjvm.reviewboard.domain.errors

abstract class ApplicationException(message: String) extends RuntimeException(message)

final case class UnauthorizedException(message: String) extends ApplicationException(message)
