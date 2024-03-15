package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.config.{Configs, OpenAIConfig}
import com.misterjvm.reviewboard.http.endpoints.OpenAIEndpoints
import com.misterjvm.reviewboard.http.requests.CompletionRequest
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.Uri
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

trait OpenAIService {
  def getCompletion(prompt: String): Task[Option[String]]
}

class OpenAIServiceLive private (
    backend: SttpBackend[Task, ZioStreams],
    interpreter: SttpClientInterpreter,
    config: OpenAIConfig
) extends OpenAIService
    with OpenAIEndpoints {
  // client
  // define the endpoint Tapir

  private def secureEndpointRequest[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, Any]
  ): S => I => Request[Either[E, O], Any] =
    interpreter.toSecureRequestThrowDecodeFailures(endpoint, Uri.parse(config.baseUrl).toOption)

  private def secureEndpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I): Task[O] =
    backend
      .send(secureEndpointRequest(endpoint)(config.key)(payload))
      .map(_.body)
      .absolve

  override def getCompletion(prompt: String): Task[Option[String]] =
    secureEndpointRequestZIO(completionsEndpoint)(CompletionRequest.single(prompt))
      .map(response => response.choices.map(_.message.content))
      .map(_.headOption)
}

object OpenAIServiceLive {
  val layer = ZLayer {
    for {
      backend     <- ZIO.service[SttpBackend[Task, ZioStreams]]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[OpenAIConfig]
    } yield new OpenAIServiceLive(backend, interpreter, config)
  }

  val configuredLayer =
    HttpClientZioBackend.layer() >+>
      ZLayer.succeed(SttpClientInterpreter()) >+>
      Configs.makeLayer[OpenAIConfig]("misterjvm.openai") >>> layer
}

object OpenAIServiceDemo extends ZIOAppDefault {
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    ZIO
      .service[OpenAIService]
      .flatMap(_.getCompletion("Please write a potential expansion of the acronym MJVM, in one sentence."))
      .flatMap(resp => Console.printLine(resp.toString))
      .provide(OpenAIServiceLive.configuredLayer)
}
