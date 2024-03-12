package com.misterjvm.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec, JsonDecoder, JsonEncoder}

enum MetricScore private (val score: Int, val name: String) {
  case Poor    extends MetricScore(1, "Poor")
  case Fair    extends MetricScore(2, "Fair")
  case Good    extends MetricScore(3, "Good")
  case Great   extends MetricScore(4, "Great")
  case Amazing extends MetricScore(5, "Amazing")
}

object MetricScore {
  given encoder: JsonEncoder[MetricScore] = JsonEncoder[Int].contramap {
    case Poor    => 1
    case Fair    => 2
    case Good    => 3
    case Great   => 4
    case Amazing => 5
  }

  given decoder: JsonDecoder[MetricScore] = JsonDecoder[Int].map {
    case 1 => Poor
    case 2 => Fair
    case 3 => Good
    case 4 => Great
    case 5 => Amazing
  }

  def fromInput(value: Int): MetricScore =
    if (value < 1) MetricScore.Poor
    else if (value > 5) MetricScore.Amazing
    else MetricScore.fromOrdinal(value - 1)
}
