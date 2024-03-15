package com.misterjvm.reviewboard.services

import zio.*

trait OpenAIService {
  def getCompletion(prompt: String): Task[Option[String]]
}

class OpenAIServiceLive private extends OpenAIService {
  override def getCompletion(prompt: String): Task[Option[String]] = ???
}

object OpenAIServiceLive {
  val layer = ZLayer {
    ZIO.succeed(new OpenAIServiceLive)
  }
}
