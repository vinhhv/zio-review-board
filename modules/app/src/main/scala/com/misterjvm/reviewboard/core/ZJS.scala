package com.misterjvm.reviewboard.core

import com.misterjvm.reviewboard.config.BackendClientConfig
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

object ZJS {
  def useBackend = ZIO.serviceWithZIO[BackendClient]

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A]) {
    def emitTo(eventBus: EventBus[A]) =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.fork(
          zio
            .tap(value => ZIO.attempt(eventBus.emit(value)))
            .provide(BackendClientLive.configuredLayer)
        )
      }

    def toEventStream: EventStream[A] = {
      val bus = EventBus[A]()
      emitTo(bus)
      bus.events
    }

    def runJS =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.runToFuture(zio.provide(BackendClientLive.configuredLayer))
      }
  }

  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])
    def apply(payload: I): Task[O] = {
      ZIO
        .service[BackendClient]
        .flatMap { backendClient =>
          backendClient.endpointRequestZIO(endpoint)(payload)
        }
        .provide(BackendClientLive.configuredLayer)
    }
}
