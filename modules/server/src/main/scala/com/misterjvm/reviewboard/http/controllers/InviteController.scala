package com.misterjvm.reviewboard.http.controllers

import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.http.endpoints.InviteEndpoints
import com.misterjvm.reviewboard.http.requests.InvitePackRequest
import com.misterjvm.reviewboard.http.responses.InviteResponse
import com.misterjvm.reviewboard.services.{InviteService, JWTService, PaymentService}
import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint
import zio.*

class InviteController private (inviteService: InviteService, jwtService: JWTService, paymentService: PaymentService)
    extends BaseController
    with InviteEndpoints {

  val addPack: ServerEndpoint[Any, Task] =
    addPackEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { token => request =>
        inviteService
          .addInvitePack(token.email, request.programId)
          .map(_.toString)
          .either
      }

  val invite: ServerEndpoint[Any, Task] =
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

  val getByUserId: ServerEndpoint[Any, Task] =
    getByUserIdEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { token => _ =>
        inviteService.getByUsername(token.email).either
      }

  val addPackPromoted: ServerEndpoint[Any, Task] =
    addPackPromotedEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { token => request =>
        inviteService
          .addInvitePack(token.email, request.programId)
          .flatMap { packId =>
            paymentService.createCheckoutSession(packId, token.email)
          } // Option[Session]
          .someOrFail(new RuntimeException("Cannot create payment checkout session"))
          .map(_.getUrl()) // the checkout session URL = the desired payload
          .either
      }

  val webhook: ServerEndpoint[Any, Task] =
    webhookEndpoint
      .serverLogic { (signature, payload) =>
        paymentService
          .handleWebhookEvent(signature, payload, packId => inviteService.activatePack(packId.toLong))
          .unit
          .either
      }

  override val routes: List[ServerEndpoint[Any, Task]] = List(addPack, addPackPromoted, webhook, getByUserId, invite)
}

object InviteController {
  val makeZIO = for {
    inviteService  <- ZIO.service[InviteService]
    jwtService     <- ZIO.service[JWTService]
    paymentService <- ZIO.service[PaymentService]
  } yield new InviteController(inviteService, jwtService, paymentService)
}
