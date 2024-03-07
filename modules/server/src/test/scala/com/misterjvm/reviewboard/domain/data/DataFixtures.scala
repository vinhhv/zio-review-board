package com.misterjvm.reviewboard.domain.data

import com.misterjvm.reviewboard.domain.data.{MetricScore, PaymentType, Program, Review, _}
import zio.*
import zio.test.*

import java.time.Instant

trait DataFixtures {
  // TODO: Not working right now due to some issue with dependencies ("java.lang.NoClassDefFoundError: zio/stream/ZChannel$UpstreamPullRequest")
  protected def genProgram: Gen[Any, Program] =
    for {
      slug        <- Gen.stringN(8)(Gen.alphaNumericChar)
      name        <- Gen.stringN(8)(Gen.alphaNumericChar) // <--- causing error mentioned above
      url         <- Gen.stringN(16)(Gen.alphaNumericChar)
      trainerId   <- Gen.long
      trainerName <- Gen.stringN(8)(Gen.alphaChar)
      paymentType <- Gen.fromIterable(PaymentType.values)
      tags        <- Gen.listOfN(3)(Gen.stringN(8)(Gen.alphaChar))
    } yield Program(
      id = -1L,
      slug = slug,
      name = name,
      url = url,
      trainerId = trainerId,
      trainerName = trainerName,
      paymentType = paymentType,
      tags = tags
    )

  protected def genProgramN(n: Int): ZIO[Any, Nothing, List[Program]] =
    Gen.listOfN(n)(genProgram).sample.runHead.map(_.flatten.get.value)

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
