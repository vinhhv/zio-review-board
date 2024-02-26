package com.misterjvm.reviewboard.http.endpoints

import sttp.tapir.*

trait HealthEndpoint {
  val healthEndpoint = endpoint
    .tag("health")
    .name("health")
    .description("health check")
    .get
    .in("health")
    .out(plainBody[String])

  val errorEndpoint =
    endpoint
      .tag("health")
      .name("error health")
      .description("health check - should fail")
      .get
      .in("health" / "error")
      .out(plainBody[String])
}
