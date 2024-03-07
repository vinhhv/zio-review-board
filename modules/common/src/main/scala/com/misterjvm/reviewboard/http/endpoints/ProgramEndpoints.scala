package com.misterjvm.reviewboard.http.endpoints

import com.misterjvm.reviewboard.domain.data.{Program, ProgramFilter}
import com.misterjvm.reviewboard.http.requests.CreateProgramRequest
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import zio.*

trait ProgramEndpoints extends BaseEndpoint {
  val createEndpoint =
    secureBaseEndpoint
      .tag("programs")
      .name("create")
      .description("create a listing for a program")
      .in("programs")
      .post
      .in(jsonBody[CreateProgramRequest])
      .out(jsonBody[Program])

  val getAllEndpoint =
    baseEndpoint
      .tag("programs")
      .name("getAll")
      .description("get all program listings")
      .in("programs")
      .get
      .out(jsonBody[List[Program]])

  val getByIdEndpoint =
    baseEndpoint
      .tag("programs")
      .name("getById")
      .description("get program by its id (or maybe by slug?)") // TODO
      .in("programs" / path[String]("id"))
      .get
      .out(jsonBody[Option[Program]])

  val allFiltersEndpoint =
    baseEndpoint
      .tag("Programs")
      .name("allFilters")
      .description("Get all possible search filters")
      .in("programs" / "filters")
      .get
      .out(jsonBody[ProgramFilter])
}
