package com.misterjvm.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}

import scala.util.Try

enum PaymentType private (val id: Int, val description: String):
  case LifetimeAccess               extends PaymentType(0, "One-time payment for lifetime access")
  case Subscription                 extends PaymentType(1, "Subscription-based (monthly or yearly)")
  case SubscriptionOrLifetimeAccess extends PaymentType(2, "Subscription or one-time payment for lifetime access")

  def fromString(paymentTypeStr: String): PaymentType =
    Try(PaymentType.valueOf(paymentTypeStr)).fold(_ => LifetimeAccess, pt => pt)

object PaymentType {
  given encoder: JsonEncoder[PaymentType] = JsonEncoder[Int].contramap {
    case LifetimeAccess               => 0
    case Subscription                 => 1
    case SubscriptionOrLifetimeAccess => 2
  }

  given decoder: JsonDecoder[PaymentType] = JsonDecoder[Int].map {
    case 0 => LifetimeAccess
    case 1 => Subscription
    case 2 => SubscriptionOrLifetimeAccess
  }
}
