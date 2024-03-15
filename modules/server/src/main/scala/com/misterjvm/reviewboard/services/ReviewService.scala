package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.domain.data.{Review, ReviewSummary}
import com.misterjvm.reviewboard.http.requests.CreateReviewRequest
import com.misterjvm.reviewboard.repositories.ReviewRepository
import zio.*

import java.time.Instant

trait ReviewService {
  def create(request: CreateReviewRequest, userId: Long): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByProgramId(programId: Long): Task[List[Review]]
  def getByProgramSlug(programSlug: String): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def getSummary(programId: Long): Task[Option[ReviewSummary]]
  def makeSummary(programId: Long): Task[Option[ReviewSummary]]
}

class ReviewServiceLive private (repo: ReviewRepository) extends ReviewService {
  override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
    repo.create(
      Review(
        id = -1L,
        programId = request.programId,
        programSlug = request.programSlug,
        userId = userId,
        value = request.value,
        quality = request.quality,
        content = request.content,
        userExperience = request.userExperience,
        accessibility = request.accessibility,
        support = request.support,
        wouldRecommend = request.wouldRecommend,
        review = request.review,
        created = Instant.now(),
        updated = Instant.now()
      )
    )

  override def getById(id: Long): Task[Option[Review]] =
    repo.getById(id)

  override def getByProgramId(programId: Long): Task[List[Review]] =
    repo.getByProgramId(programId)

  override def getByProgramSlug(programSlug: String): Task[List[Review]] =
    repo.getByProgramSlug(programSlug)

  override def getByUserId(userId: Long): Task[List[Review]] =
    repo.getByUserId(userId)

  override def getSummary(programId: Long): Task[Option[ReviewSummary]] = ???

  override def makeSummary(programId: Long): Task[Option[ReviewSummary]] = ???
}

object ReviewServiceLive {
  val layer = ZLayer {
    ZIO.service[ReviewRepository].map(repo => new ReviewServiceLive(repo))
  }
}
