package com.misterjvm.reviewboard.core

import com.misterjvm.reviewboard.common.Constants
import com.misterjvm.reviewboard.config.BackendClientConfig
import com.misterjvm.reviewboard.http.endpoints.*
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

final case class RestrictedEndpointException(msg: String) extends RuntimeException

trait BackendClient {
  val program: ProgramEndpoints
  val user: UserEndpoints
  val review: ReviewEndpoints
  val invite: InviteEndpoints

  def endpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O]

  def secureEndpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[String, I, E, O, Any])(payload: I): Task[O]
}

class BackendClientLive(
    backend: SttpBackend[Task, ZioStreams & WebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig
) extends BackendClient {
  override val program: ProgramEndpoints = new ProgramEndpoints {}
  override val user: UserEndpoints       = new UserEndpoints {}
  override val review: ReviewEndpoints   = new ReviewEndpoints {}
  override val invite: InviteEndpoints   = new InviteEndpoints {}

  private def endpointRequest[I, E, O](endpoint: Endpoint[Unit, I, E, O, Any]): I => Request[Either[E, O], Any] =
    interpreter.toRequestThrowDecodeFailures(endpoint, config.uri)

  private def secureEndpointRequest[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, Any]
  ): S => I => Request[Either[E, O], Any] =
    interpreter.toSecureRequestThrowDecodeFailures(endpoint, config.uri)

  override def endpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O] =
    backend
      .send(endpointRequest(endpoint)(payload))
      .map(_.body)
      .absolve

  private def tokenOrFail =
    ZIO
      .fromOption(Session.getUserState)
      .orElseFail(RestrictedEndpointException("You need to log in."))
      .map(_.token)

  override def secureEndpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I): Task[O] =
    for {
      token <- tokenOrFail
      response <-
        backend
          .send(secureEndpointRequest(endpoint)(token)(payload))
          .map(_.body)
          .absolve
    } yield response
}

object BackendClientLive {
  val layer = ZLayer {
    for {
      backend     <- ZIO.service[SttpBackend[Task, ZioStreams & WebSockets]]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[BackendClientConfig]
    } yield new BackendClientLive(backend, interpreter, config)
  }

  val configuredLayer = {
    val backend                            = FetchZioBackend()
    val interpreter: SttpClientInterpreter = SttpClientInterpreter()
    val config                             = BackendClientConfig(Some(uri"${Constants.backendBaseUrl}"))

    ZLayer.succeed(backend) ++
      ZLayer.succeed(interpreter) ++
      ZLayer.succeed(config) >>> layer
  }
}
