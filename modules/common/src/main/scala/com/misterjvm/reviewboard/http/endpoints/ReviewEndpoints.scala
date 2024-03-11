package com.misterjvm.reviewboard.http.endpoints

import com.misterjvm.reviewboard.domain.data.Review
import com.misterjvm.reviewboard.http.requests.CreateReviewRequest
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import zio.*

trait ReviewEndpoints extends BaseEndpoint {
  // post /reviews { CreateReviewRequest } - create review
  // returns a Review
  val createEndpoint =
    secureBaseEndpoint
      .tag("Reviews")
      .name("create")
      .description("Add a review for a company")
      .in("reviews")
      .post
      .in(jsonBody[CreateReviewRequest])
      .out(jsonBody[Review])

  // get /reviews/id - get review by id
  // returns Option[Review]
  val getByIdEndpoint =
    baseEndpoint
      .tag("Reviews")
      .name("getById")
      .description("Get a review by its id")
      .in("reviews" / path[Long]("id"))
      .get
      .out(jsonBody[Option[Review]])

  // get /reviews/program/id - get review by program id
  // returns List[Review]
  val getByProgramIdEndpoint =
    baseEndpoint
      .tag("Reviews")
      .name("getByProgramId")
      .description("Get reviews for a program")
      .in("reviews" / "program" / path[Long]("id"))
      .get
      .out(jsonBody[List[Review]])

  val getByProgramSlugEndpoint =
    baseEndpoint
      .tag("Reviews")
      .name("getByProgramSlug")
      .description("Get reviews for a program by program slug")
      .in("reviews" / "program" / "slug" / path[String]("slug"))
      .get
      .out(jsonBody[List[Review]])

}
