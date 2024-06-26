package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.config.SummaryConfig
import com.misterjvm.reviewboard.domain.data.{DataFixtures, Review, ReviewSummary}
import com.misterjvm.reviewboard.http.requests.CreateReviewRequest
import com.misterjvm.reviewboard.repositories.ReviewRepository
import zio.*
import zio.test.*

import java.time.Instant

object ReviewServiceSpec extends ZIOSpecDefault with DataFixtures {

  val stubRepoLayer = ZLayer.succeed {
    new ReviewRepository {
      override def create(review: Review): Task[Review] =
        ZIO.succeed(goodReview)

      override def getById(id: Long): Task[Option[Review]] =
        ZIO.succeed {
          id match {
            case 1 => Some(goodReview)
            case 2 => Some(badReview)
            case _ => None
          }
        }

      override def getByProgramId(programId: Long): Task[List[Review]] = ZIO.succeed {
        if (programId == 1) List(goodReview, badReview)
        else List()
      }

      override def getByUserId(userId: Long): Task[List[Review]] = ZIO.succeed {
        if (userId == 1) List(goodReview, badReview)
        else List()
      }

      override def update(id: Long, op: Review => Review): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"Update failed: ID $id not found")).map(op)

      override def delete(id: Long): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"Delete failed: ID $id not found"))

      override def getByProgramSlug(programSlug: String): Task[List[Review]] =
        ZIO.succeed {
          if (programSlug == goodReview.programSlug) List(goodReview)
          else List()
        }

      override def getSummary(programId: Long): Task[Option[ReviewSummary]] =
        ZIO.none

      override def insertSummary(programId: Long, summary: String): Task[ReviewSummary] =
        ZIO.succeed(ReviewSummary(programId, summary, Instant.now(), Instant.now()))
    }

  }

  val stubReviewSummaryService = ZLayer.succeed {
    new OpenAIService {
      override def getCompletion(prompt: String): Task[Option[String]] = ZIO.none
    }
  }

  val summaryConfigLayer = ZLayer.succeed {
    SummaryConfig(3, 20, 86400)
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewServiceSpec")(
      test("create") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.create(
            CreateReviewRequest(
              programId = goodReview.programId,
              programSlug = goodReview.programSlug,
              value = goodReview.value,
              quality = goodReview.quality,
              content = goodReview.content,
              userExperience = goodReview.userExperience,
              accessibility = goodReview.accessibility,
              support = goodReview.support,
              wouldRecommend = goodReview.wouldRecommend,
              review = goodReview.review
            ),
            userId = 1L
          )
        } yield assertTrue(
          review.programId == goodReview.programId &&
            review.value == goodReview.value &&
            review.quality == goodReview.quality &&
            review.content == goodReview.content &&
            review.userExperience == goodReview.userExperience &&
            review.accessibility == goodReview.accessibility &&
            review.support == goodReview.support &&
            review.wouldRecommend == goodReview.wouldRecommend &&
            review.review == goodReview.review
        )
      },
      test("getById") {
        for {
          service        <- ZIO.service[ReviewService]
          review         <- service.getById(1L)
          reviewNotFound <- service.getById(999L)
        } yield assertTrue {
          review.contains(goodReview) && reviewNotFound.isEmpty
        }
      },
      test("getByProgramId") {
        for {
          service         <- ZIO.service[ReviewService]
          reviews         <- service.getByProgramId(1L)
          reviewsNotFound <- service.getByProgramId(999L)
        } yield assertTrue {
          reviews.toSet == Set(goodReview, badReview) && reviewsNotFound.isEmpty
        }
      },
      test("getByUserId") {
        for {
          service         <- ZIO.service[ReviewService]
          reviews         <- service.getByUserId(1L)
          reviewsNotFound <- service.getByUserId(999L)
        } yield assertTrue {
          reviews.toSet == Set(goodReview, badReview) && reviewsNotFound.isEmpty
        }
      }
    ).provide(ReviewServiceLive.layer, stubRepoLayer, stubReviewSummaryService, summaryConfigLayer)
}
