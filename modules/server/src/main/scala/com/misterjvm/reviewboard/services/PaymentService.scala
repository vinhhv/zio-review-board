package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.config.{Configs, StripeConfig}
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.Stripe as TheStripe
import zio.*

import scala.jdk.OptionConverters.*

trait PaymentService {
  // create a session
  def createCheckoutSession(invitePackId: Long, username: String): Task[Option[Session]]
  // handle webhook
  def handleWebhookEvent[A](signature: String, payload: String, action: String => Task[A]): Task[Option[A]]
}

class PaymentServiceLive private (config: StripeConfig) extends PaymentService {
  override def createCheckoutSession(invitePackId: Long, username: String): Task[Option[Session]] =
    ZIO
      .attempt {
        SessionCreateParams
          .builder()
          .setMode(SessionCreateParams.Mode.PAYMENT)
          .setSuccessUrl(config.successUrl)
          .setCancelUrl(config.cancelUrl)
          .setCustomerEmail(username)
          .setClientReferenceId(invitePackId.toString) // own payload to reference invite pack
          .setInvoiceCreation(
            SessionCreateParams.InvoiceCreation
              .builder()
              .setEnabled(true)
              .build()
          )
          .setPaymentIntentData(
            SessionCreateParams.PaymentIntentData
              .builder()
              .setReceiptEmail(username)
              .build()
          )
          .addLineItem(
            SessionCreateParams.LineItem
              .builder()
              .setPrice(config.price) // unique ID of your Stripe product
              .setQuantity(1L)
              .build()
          )
          .build()
      }
      .map(params => Session.create(params))
      .map(Option(_))
      .logError("Stripe session creation FAILED")
      .catchSome { case _ =>
        ZIO.none
      }

  override def handleWebhookEvent[A](
      signature: String,
      payload: String,
      action: String => Task[A]
  ): Task[Option[A]] =
    ZIO
      .attempt {
        Webhook.constructEvent(payload, signature, config.secret)
      }
      .flatMap { event =>
        event.getType() match {
          case "checkout.session.completed" =>
            ZIO.foreach(
              event
                .getDataObjectDeserializer()
                // Make sure API matches between build.sbt and API version for API key, or else silent failure (returns None)
                .getObject()
                .toScala
                .map(_.asInstanceOf[Session])
                .map(_.getClientReferenceId())
            )(action)
          case _ => ZIO.none
        }

      }
}

object PaymentServiceLive {
  val layer = ZLayer {
    for {
      config <- ZIO.service[StripeConfig]
      _      <- ZIO.attempt(TheStripe.apiKey = config.key)
    } yield new PaymentServiceLive(config)
  }

  val configuredLayer =
    Configs.makeLayer[StripeConfig]("misterjvm.stripe") >>> layer
}
