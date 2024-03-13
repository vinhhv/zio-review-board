package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.http.endpoints.InviteEndpoints
import com.misterjvm.reviewboard.http.requests.InvitePackRequest
import com.misterjvm.reviewboard.http.responses.InviteResponse
import com.misterjvm.reviewboard.services.{InviteService, JWTService}
import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint
import zio.*

class InviteController private (inviteService: InviteService, jwtService: JWTService)
    extends BaseController
    with InviteEndpoints {

  val addPack =
    addPackEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { token => request =>
        inviteService
          .addInvitePack(token.email, request.programId)
          .map(_.toString)
          .either
      }

  val invite =
    invitedEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { token => request =>
        inviteService
          .sendInvites(token.email, request.programId, request.emails)
          .map { nInvitesSent =>
            if (nInvitesSent == request.emails.size)
              InviteResponse("OK", nInvitesSent)
            else
              InviteResponse("Partial Success", nInvitesSent)
          }
          .either
      }

  val getByUserId =
    getByUserIdEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { token => _ =>
        inviteService.getByUsername(token.email).either
      }

  override val routes: List[ServerEndpoint[Any, Task]] = List(addPack, invite, getByUserId)
}

object InviteController {
  val makeZIO = for {
    inviteService <- ZIO.service[InviteService]
    jwtService    <- ZIO.service[JWTService]
  } yield new InviteController(inviteService, jwtService)
}
