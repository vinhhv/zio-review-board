package com.misterjvm.reviewboard.config

final case class StripeConfig(
    key: String,
    secret: String, // webhook secret
    price: String,  // price ID in Stripe
    successUrl: String,
    cancelUrl: String
)
