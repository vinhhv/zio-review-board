package com.misterjvm.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}

sealed trait ProgramType

object ProgramType {
  case object LifetimeAccess               extends ProgramType
  case object Subscription                 extends ProgramType
  case object SubscriptionOrLifetimeAccess extends ProgramType

  given encoder: JsonEncoder[ProgramType] = JsonEncoder[String].contramap {
    case LifetimeAccess               => "LifetimeAccess"
    case Subscription                 => "Subscription"
    case SubscriptionOrLifetimeAccess => "SubscriptionOrLifetimeAccess"
  }

  given decoder: JsonDecoder[ProgramType] = JsonDecoder[String].map {
    case "LifetimeAccess"               => LifetimeAccess
    case "Subscription"                 => Subscription
    case "SubscriptionOrLifetimeAccess" => SubscriptionOrLifetimeAccess
  }
}
