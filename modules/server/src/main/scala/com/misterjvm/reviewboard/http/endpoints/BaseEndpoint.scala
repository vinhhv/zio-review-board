package com.misterjvm.reviewboard.http.endpoints

import com.misterjvm.reviewboard.domain.errors.HttpError
import sttp.tapir.*

trait BaseEndpoint {
  val baseEndpoint = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

  val secureBaseEndpoint =
    baseEndpoint
      .securityIn(auth.bearer[String]()) // header "Authorization: Bearer ..."
}
