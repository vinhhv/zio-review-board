package com.misterjvm.reviewboard.domain.data

import com.misterjvm.reviewboard.domain.data.*
import zio.*
import zio.test.*

import java.time.Instant
import com.misterjvm.reviewboard.domain.data.{MetricScore, PaymentType, Program, Review}

trait DataFixtures {
  protected def genProgram: Gen[Any, Program] =
    for {
      slug        <- Gen.stringN(8)(Gen.alphaNumericChar)
      name        <- Gen.stringN(8)(Gen.alphaNumericChar)
      url         <- Gen.stringN(16)(Gen.alphaNumericChar)
      trainerId   <- Gen.long
      paymentType <- Gen.fromIterable(PaymentType.values)
    } yield Program(
      id = -1L,
      slug = slug,
      name = name,
      url = url,
      trainerId = trainerId,
      paymentType = paymentType
    )

  protected def genProgramN(n: Int): ZIO[Any, Nothing, List[Program]] =
    Gen.listOfN(n)(genProgram).sample.runHead.map(_.get.value)

  import MetricScore.*
  val goodReview = Review(
    id = 1L,
    programId = 1L,
    userId = 1L,
    value = Amazing,
    quality = Amazing,
    content = Amazing,
    userExperience = Amazing,
    accessibility = Amazing,
    support = Amazing,
    wouldRecommend = Amazing,
    review = "Wow!",
    created = Instant.now(),
    updated = Instant.now()
  )

  val badReview = Review(
    id = 2L,
    programId = 1L,
    userId = 1L,
    value = Poor,
    quality = Poor,
    content = Poor,
    userExperience = Poor,
    accessibility = Poor,
    support = Poor,
    wouldRecommend = Poor,
    review = "Sucks!",
    created = Instant.now(),
    updated = Instant.now()
  )

}
