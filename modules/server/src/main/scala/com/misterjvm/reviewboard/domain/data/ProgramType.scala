package com.misterjvm.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}

enum ProgramType:
  case LifetimeAccess, Subscription, SubscriptionOrLifetimeAccess

object ProgramType {
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
