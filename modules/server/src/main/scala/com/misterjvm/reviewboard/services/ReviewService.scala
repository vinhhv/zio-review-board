package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.domain.data.Review
import com.misterjvm.reviewboard.http.requests.CreateReviewRequest
import com.misterjvm.reviewboard.repositories.ReviewRepository
import zio.*

import java.time.Instant

trait ReviewService {
  def create(request: CreateReviewRequest, userId: Long): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByProgramId(programId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
}

class ReviewServiceLive private (repo: ReviewRepository) extends ReviewService {
  override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
    repo.create(
      Review(
        id = -1L,
        programId = request.programId,
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

  override def getByUserId(userId: Long): Task[List[Review]] =
    repo.getByUserId(userId)
}

object ReviewServiceLive {
  val layer = ZLayer {
    ZIO.service[ReviewRepository].map(repo => new ReviewServiceLive(repo))
  }
}
