package com.misterjvm.reviewboard.http.endpoints

import com.misterjvm.reviewboard.domain.data.InviteNamedRecord
import com.misterjvm.reviewboard.http.requests.*
import com.misterjvm.reviewboard.http.responses.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait InviteEndpoints extends BaseEndpoint {

  /**
   * POST /invite/add - 200 emails to invite people to leave reviews
   * input { programId } 
   * output { packId }
   */
  val addPackEndpoint =
    secureBaseEndpoint
      .tag("invites")
      .name("add invites")
      .description("Get invite tokens")
      .in("invite" / "add")
      .post
      .in(jsonBody[InvitePackRequest])
      .out(stringBody)

  /**
   * POST /invite
   * input { [emails], programId }
   * output { nInvites, status }
   * 
   * sends emails to users
   */
  val invitedEndpoint =
    secureBaseEndpoint
      .tag("invites")
      .name("invite")
      .description("Send poeple emails inviting them to leave a review")
      .in("invite")
      .post
      .in(jsonBody[InviteRequest])
      .out(jsonBody[InviteResponse])

  /**
   * GET /invite/all
   * output [ { programId, programName, nInvites } ]
   */
  val getByUserIdEndpoint =
    secureBaseEndpoint
      .tag("invites")
      .name("get by user id")
      .description("Get all active invite packs for a user")
      .get
      .in("invite" / "all")
      .out(jsonBody[List[InviteNamedRecord]])

  // TODO: paid endpoints
}
