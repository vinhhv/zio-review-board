package com.misterjvm.reviewboard.repositories

import zio.*
import zio.test.*
import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.syntax.*

import java.time.Instant

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  override val initScript: String = "sql/reviews.sql"

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

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewRepositorySpec")(
      test("create review") {
        val program = for {
          repo   <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
        } yield review

        program.assert { review =>
          review.value == goodReview.value &&
          review.quality == goodReview.quality &&
          review.content == goodReview.content &&
          review.userExperience == goodReview.userExperience &&
          review.accessibility == goodReview.accessibility &&
          review.support == goodReview.support &&
          review.wouldRecommend == goodReview.wouldRecommend &&
          review.review == goodReview.review
        }
      },
      test("get review by different ids (id, program id, user id)") {
        for {
          repo               <- ZIO.service[ReviewRepository]
          review             <- repo.create(goodReview)
          fetchedById        <- repo.getById(review.id)
          fetchedByProgramId <- repo.getByProgramId(review.programId)
          fetchedByUserId    <- repo.getByUserId(review.userId)
        } yield assertTrue(
          fetchedById.contains(review) &&
            fetchedByProgramId.contains(review) &&
            fetchedByUserId.contains(review)
        )
      },
      test("get all") {
        for {
          repo               <- ZIO.service[ReviewRepository]
          reviewGood         <- repo.create(goodReview)
          reviewBad          <- repo.create(badReview)
          reviewsByProgramId <- repo.getByProgramId(reviewGood.programId)
          reviewsByUserId    <- repo.getByProgramId(reviewBad.programId)
        } yield assertTrue(
          reviewsByProgramId.toSet == Set(reviewGood, reviewBad) &&
            reviewsByUserId.toSet == Set(reviewGood, reviewBad)
        )
      },
      test("update") {
        for {
          repo    <- ZIO.service[ReviewRepository]
          review  <- repo.create(goodReview)
          updated <- repo.update(review.id, _.copy(review = "not too bad"))
        } yield assertTrue(
          review.value == updated.value &&
            review.quality == updated.quality &&
            review.content == updated.content &&
            review.userExperience == updated.userExperience &&
            review.accessibility == updated.accessibility &&
            review.support == updated.support &&
            review.wouldRecommend == updated.wouldRecommend &&
            updated.review == "not too bad" &&
            review.created == updated.created &&
            review.updated != updated.updated
        )
      },
      test("delete review") {
        for {
          repo        <- ZIO.service[ReviewRepository]
          review      <- repo.create(goodReview)
          _           <- repo.delete(review.id)
          maybeReview <- repo.getById(review.id)
        } yield assertTrue(maybeReview.isEmpty)
      }
    ).provide(
      ReviewRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
}
