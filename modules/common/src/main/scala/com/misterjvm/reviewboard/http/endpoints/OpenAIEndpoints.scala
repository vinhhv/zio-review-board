package com.misterjvm.reviewboard.http.endpoints

import com.misterjvm.reviewboard.domain.errors.HttpError
import com.misterjvm.reviewboard.http.requests.CompletionRequest
import com.misterjvm.reviewboard.http.responses.CompletionResponse
import sttp.client3.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait OpenAIEndpoints extends BaseEndpoint {
  val completionsEndpoint =
    endpoint
      .errorOut(statusCode and plainBody[String])
      .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)
      .securityIn(auth.bearer[String]())
      .in("v1" / "chat" / "completions")
      .post
      .in(jsonBody[CompletionRequest])
      .out(jsonBody[CompletionResponse])

}
