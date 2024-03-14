package com.misterjvm.reviewboard.config

final case class StripeConfig(
    price: String, // price ID in Stripe
    key: String,
    successUrl: String,
    cancelUrl: String
)
